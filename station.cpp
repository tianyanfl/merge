#include "station.h"
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>
#include <ctime>
#include <set>
#include <algorithm>
#include <unistd.h>
#include <vector>
#include <cstring>
#include <cstdlib>
#include <sys/socket.h>
#include <arpa/inet.h>

#define MAX_CONNECTIONS 10
#define BUFFER_SIZE 1024

using namespace std;

const string STATION_NAME_REQUEST = "STATION_NAME_REQUEST";
const string STATION_NAME_RESPONSE = "STATION_NAME_RESPONSE";
const string ROUTE_REQUEST = "ROUTE_REQUEST";
const string ROUTE_RESPONSE = "ROUTE_RESPONSE";

const std::vector<std::string> stationlist = {"TerminalA", "BusportC", "JunctionB", "BusportA"};
//const std::string local_station = "TerminalA";


Station::Station(const string &name, int http_port, int udp_port, const vector<pair<string, int>> &neighbors)
        : pool(2 * max((int) thread::hardware_concurrency(), 1)) {
    this->name = name;

    this->http_port = http_port;
//    this->http_service = socket(AF_INET, SOCK_STREAM, 0);
//    sockaddr_in http_address = addrpair_to_sockaddr(make_pair("", http_port));
//    bind(http_service, (sockaddr*) &http_address, sizeof(http_address));
//    listen(http_service, 5);

    this->udp_port = udp_port;
    this->udp_service = socket(AF_INET, SOCK_DGRAM, 0);
    sockaddr_in udp_address = addrpair_to_sockaddr(make_pair("", udp_port));
    bind(udp_service, (sockaddr * ) & udp_address, sizeof(udp_address));

    this->context_id = 1;

    this->neighbors = neighbors;
}

void Station::start() {
    ThreadPool executor(3);
    executor.enqueue([this] { this->tcp_server(this->http_port); });
    executor.enqueue([this] { this->start_udp_service(); });
    executor.enqueue([this] { this->start_station_name_service(); });
}

void Station::start_udp_service() {
    while (true) {
        char data[1024 * 1024] = {0};
        sockaddr_in sockaddr_;
        socklen_t sockaddr_len = sizeof(sockaddr_);
        recvfrom(udp_service, data, sizeof(data), 0, (sockaddr * ) & sockaddr_, &sockaddr_len);
        pair<string, int> address = sockaddr_to_addrpair(sockaddr_);
        if (find(neighbors.begin(), neighbors.end(), address) != neighbors.end()) {
            this->pool.enqueue([this, data, address] { this->handle_udp(data, address); });
        }
    }
}

void Station::handle_udp(const string &request, const pair<string, int> &address) {
    vector<string> splits = split(request, "\n", 1);
    if (splits[0] == STATION_NAME_REQUEST) {
        cout << "RECV " << STATION_NAME_REQUEST << endl;
        string body = STATION_NAME_RESPONSE + "\n" + name + "\n";
        sockaddr_in sockaddr_ = addrpair_to_sockaddr(address);
        sendto(udp_service, body.c_str(), body.length(), 0, (sockaddr * ) & sockaddr_, sizeof(sockaddr_));
        cout << "SEND " << STATION_NAME_RESPONSE << endl;
    } else if (splits[0] == STATION_NAME_RESPONSE) {
        cout << "RECV " << STATION_NAME_RESPONSE << endl;
        string body = splits[1];
        lock_guard<mutex> lock(neighbor_lock);
        neighbor_dict[body.substr(0, body.length() - 1)] = address;
    } else if (splits[0] == ROUTE_REQUEST) {
        cout << "RECV " << ROUTE_REQUEST << endl;
        unique_ptr<UDPRouteDiagram> diagram = UDPRouteDiagram::from_str(request);
        TimeTable time_table = get_time_table();
        shared_ptr<Route> route = get_route(time_table, diagram->destination, diagram->minute);

        unique_ptr<UDPRouteDiagram> response = nullptr;
        if (route) {
            vector<string> from_list(diagram->from_list);
            from_list.push_back(name);
            response = make_unique<UDPRouteDiagram>(ROUTE_RESPONSE, diagram->context_id, route->arrival_time, from_list,
                                                    diagram->destination, vector<string>{route->to_string()});
        } else {
            response = request_route(*diagram.get(), time_table);
        }
        sockaddr_in sockaddr_ = addrpair_to_sockaddr(address);
        string body = response->to_string();
        sendto(udp_service, body.c_str(), body.length(), 0, (sockaddr * ) & sockaddr_, sizeof(sockaddr_));
        cout << "SEND " << ROUTE_RESPONSE << endl;
    } else if (splits[0] == ROUTE_RESPONSE) {
        cout << "RECV " << ROUTE_RESPONSE << endl;
        unique_ptr<UDPRouteDiagram> diagram = UDPRouteDiagram::from_str(request);
        lock_guard<mutex> lock(route_response_lock);
        route_response_dict[diagram->context_str()].push_back(move(diagram));
    }
}

void Station::start_station_name_service() {
    while (neighbor_dict.size() < neighbors.size()) {
        set<pair<string, int>> neighbor_set;
        for (auto &kv: neighbor_dict) {
            neighbor_set.insert(kv.second);
        }
        for (auto &nb: neighbors) {
            if (neighbor_set.find(nb) == neighbor_set.end()) {
                string body = STATION_NAME_REQUEST + "\n";
                sockaddr_in sockaddr_ = addrpair_to_sockaddr(nb);
                sendto(udp_service, body.c_str(), body.length(), 0, (sockaddr * ) & sockaddr_, sizeof(sockaddr_));
                cout << "SEND " << STATION_NAME_REQUEST << " to " << nb.first << ":" << nb.second << endl;
            }
        }
        this_thread::sleep_for(chrono::milliseconds(100));
    }
}

unique_ptr<UDPRouteDiagram> Station::request_route(UDPRouteDiagram &from_diagram, TimeTable &time_table) {
    from_diagram.from_list.push_back(name);
    unordered_map<string, shared_ptr<Route>> route_dict;
    for (auto &nb: neighbor_dict) {
        const string &station_name = nb.first;
        const pair<string, int> &address = nb.second;
        if (find(from_diagram.from_list.begin(), from_diagram.from_list.end(), station_name) !=
            from_diagram.from_list.end()) {
            continue;
        }
        shared_ptr<Route> route = get_route(time_table, station_name, from_diagram.minute);
        if (!route) {
            continue;
        }
        route_dict[station_name] = route;
        auto diagram = make_unique<UDPRouteDiagram>(ROUTE_REQUEST, from_diagram.context_id, route->arrival_time + 1,
                                                    from_diagram.from_list, from_diagram.destination, vector<string>());
        sockaddr_in sockaddr_ = addrpair_to_sockaddr(address);
        string body = diagram->to_string();
        sendto(udp_service, body.c_str(), body.length(), 0, (sockaddr * ) & sockaddr_, sizeof(sockaddr_));
        cout << "SEND " << ROUTE_REQUEST << endl;
    }

    string context_str = from_diagram.context_str();
    int repeat_times = 60;
    while (route_response_dict[context_str].size() < route_dict.size()) {
        this_thread::sleep_for(chrono::milliseconds(50));
        repeat_times--;
        if (repeat_times == 0) {
            break;
        }
    }

    if (route_response_dict[context_str].size() == 0) {
        return make_unique<UDPRouteDiagram>(ROUTE_RESPONSE, from_diagram.context_id, Route::TIME_NONE,
                                            from_diagram.from_list, from_diagram.destination, vector<string>());
    }

    vector<shared_ptr<UDPRouteDiagram>> diagrams = route_response_dict[context_str];
    route_response_lock.lock();
    route_response_dict.erase(context_str);
    route_response_lock.unlock();
    sort(diagrams.begin(), diagrams.end(), [](shared_ptr<UDPRouteDiagram> a, shared_ptr<UDPRouteDiagram> b) {
        return a->minute < b->minute;
    });
    if (diagrams.size() == 0) {
        return make_unique<UDPRouteDiagram>(ROUTE_RESPONSE, from_diagram.context_id, Route::TIME_NONE,
                                            from_diagram.from_list, from_diagram.destination, vector<string>());
    }

    vector<string> route_list;
    route_list.push_back(route_dict[diagrams[0]->from_list.back()]->to_string());
    route_list.insert(route_list.end(), diagrams[0]->route_list.begin(), diagrams[0]->route_list.end());
    return make_unique<UDPRouteDiagram>(ROUTE_RESPONSE, from_diagram.context_id, diagrams[0]->minute,
                                        from_diagram.from_list, from_diagram.destination, route_list);
}

TimeTable Station::get_time_table() {
    TimeTable time_table;
    ifstream file("./tt-" + name);
    if (!file.is_open()) {
        throw invalid_argument("No time table file");
    }
    string line;
    for (int i = 0; i < 3; i++) {
        getline(file, line);
    }
    while (getline(file, line)) {
        vector<string> splits = split(line, ",");
        shared_ptr<Route> route = make_shared<Route>(splits[0], name, splits[1], splits[2], splits[3], splits[4]);
        time_table[route->arrival_station].push_back(route);
    }
    for (auto &entry: time_table) {
        vector<shared_ptr<Route>> &value = entry.second;
        sort(value.begin(), value.end(), [](shared_ptr<Route> a, shared_ptr<Route> b) {
            return a->arrival_time < b->arrival_time;
        });
    }
    return time_table;
}

shared_ptr<Route> Station::get_route(TimeTable &time_table, const string &destination, int minute) {
    const vector<shared_ptr<Route>> &routes = time_table[destination];
    for (const auto &r: routes) {
        if (r->departure_time >= minute) {
            return r;
        }
    }
    return nullptr;
}

sockaddr_in Station::addrpair_to_sockaddr(const pair<string, int> &addrpair) {
    sockaddr_in sockaddr_;
    sockaddr_.sin_family = AF_INET;
    sockaddr_.sin_port = htons(addrpair.second);
    if (addrpair.first == "") {
        sockaddr_.sin_addr.s_addr = INADDR_ANY;
    } else {
        inet_aton(addrpair.first.c_str(), &sockaddr_.sin_addr);
    }
    return sockaddr_;
}

pair<string, int> Station::sockaddr_to_addrpair(const sockaddr_in &addr) {
    return make_pair(inet_ntoa(addr.sin_addr), ntohs(addr.sin_port));
}

const int Route::TIME_NONE = tstr_to_min("24:00");

Route::Route(string departure_time, string departure_station, string route_name, string departing_from,
             string arrival_time, string arrival_station) {
    this->departure_time = tstr_to_min(departure_time);
    this->departure_station = departure_station;
    this->route_name = route_name;
    this->departing_from = departing_from;
    this->arrival_time = tstr_to_min(arrival_time);
    this->arrival_station = arrival_station;
}

string Route::to_string() {
    return route_name + ": from " + departure_station + " " + departing_from + " (" + min_to_tstr(departure_time) +
           ") to " + arrival_station + " (" + min_to_tstr(arrival_time) + ")";
}

UDPRouteDiagram::UDPRouteDiagram(const string &dtype, int context_id, int minute, const vector<string> &from_list,
                                 const string &destination, const vector<string> &route_list) {
    this->dtype = dtype;
    this->context_id = context_id;
    this->minute = minute;
    this->from_list = from_list;
    this->destination = destination;
    this->route_list = route_list;
}

unique_ptr<UDPRouteDiagram> UDPRouteDiagram::from_str(const string &data) {
    vector<string> splits = split(data.substr(0, data.length() - 1), "\n");
    const string &dtype = splits[0];
    vector<string> context = split(splits[1], " ");
    int context_id = stoi(context[1]);
    const string &destination = context[2];
    int minute = tstr_to_min(splits[2]);
    vector<string> from_list = split(splits[3], " ");
    vector<string> route_list;
    if (splits.size() > 4) {
        for (size_t i = 4; i < splits.size(); i++) {
            route_list.push_back(splits[i]);
        }
    }
    return make_unique<UDPRouteDiagram>(dtype, context_id, minute, from_list, destination, route_list);
}

string UDPRouteDiagram::context_str() {
    return from_list[0] + " " + std::to_string(context_id) + " " + destination;
}

string UDPRouteDiagram::to_string() {
    string diagram = dtype + "\n";
    diagram += context_str() + "\n";
    diagram += min_to_tstr(minute) + "\n";
    diagram += join(from_list, " ") + "\n";
    if (route_list.size() > 0) {
        diagram += join(route_list, "\n") + "\n";
    }
    return diagram;
}


bool Station::check_the_station(const std::string &station) {
    return station == name;
}

std::string Station::decode_url(const std::string &input) {
    std::string output;
    for (size_t i = 0; i < input.length(); ++i) {
        if (input[i] == '%' && i + 2 < input.length() && input[i + 1] == '3' && input[i + 2] == 'A') {
            output += ':';
            i += 2;
        } else {
            output += input[i];
        }
    }
    return output;
}

void Station::generate_station_list(std::string &response) {
    response = "<select name=\"to\" id=\"to\">";
    for (const auto &station: stationlist) {
        response += "<option value=\"" + station + "\">" + station + "</option>";
    }
    response += "</select>";
}

void Station::handle_request(int client_socket) {
    char buffer[BUFFER_SIZE];
    int bytes_received = recv(client_socket, buffer, BUFFER_SIZE - 1, 0);
    if (bytes_received <= 0) {
        std::cerr << "Client disconnected or error receiving data\n";
        close(client_socket);
        return;
    }

    buffer[bytes_received] = '\0'; // Null-terminate received data
    std::cout << "Received data: " << buffer << std::endl;

    std::string request(buffer);
    std::istringstream request_stream(request);
    std::string request_line;
    std::getline(request_stream, request_line);

    std::istringstream line_stream(request_line);
    std::string method, path;
    line_stream >> method >> path;

    if (method != "GET") {
        std::string response = "HTTP/1.1 405 Method Not Allowed\r\n\r\n";
        send(client_socket, response.c_str(), response.length(), 0);
        close(client_socket);
        return;
    }

    std::string to, time;
    size_t pos = path.find('?');
    if (pos != std::string::npos) {
        std::string query = path.substr(pos + 1);
        std::istringstream query_stream(query);
        std::string param;
        while (std::getline(query_stream, param, '&')) {
            size_t eq_pos = param.find('=');
            if (eq_pos != std::string::npos) {
                std::string key = param.substr(0, eq_pos);
                std::string value = param.substr(eq_pos + 1);
                if (key == "to") {
                    to = value;
                } else if (key == "time") {
                    time = decode_url(value);
                }
            }
        }
    }

    std::string station_list = "";
    generate_station_list(station_list);

    std::string response_content;
    response_content = "<h1>Request Form</h1>"
                       "<form method='GET' action='/'>"
                       "<label for='to'>Destination:</label>" + station_list +
                       "<label for='time'>Time:</label>"
                       "<input type='time' id='time' name='time'><br>"
                       "<input type='submit' value='Submit'></form>";

    if (!to.empty() || !time.empty()) {
        response_content = "<h1>Return</h1>";
        if (!to.empty()) {
            if (check_the_station(to)) {
                response_content += "<p>Invalid station: " + to + "</p>";
            } else {
                response_content += "<p>Destination: " + to + "</p>";
            }
        }
        if (!time.empty()) {
            response_content += "<p>Requested Time: " + time + "</p>";
            // const std::string route = find_route(time, to); // waiting for partner's code
            // response_content += "<p>Route: " + route + "</p>";
            int minute = std::stoi(time);
            TimeTable time_table = get_time_table();
            shared_ptr<Route> route = get_route(time_table, to, minute);
            string body;
            if (route) {
                body = route->to_string();
            } else {
                context_lock.lock();
                int id = context_id;
                context_id++;
                context_lock.unlock();

                UDPRouteDiagram request_diagram(STATION_NAME_REQUEST, id, minute, vector<string>(), to,
                                                vector<string>());
                unique_ptr<UDPRouteDiagram> response = request_route(request_diagram, time_table);
                if (response->minute == Route::TIME_NONE) {
                    body = "No route";
                } else {
                    body = join(response->route_list, "\n");
                }
            }
            response_content += body;
        } else {
            response_content += "<p>Please input the time</p>";
        }
    }

    std::string http_response;
    http_response = "HTTP/1.1 200 OK\r\n"
                    "Content-Type: text/html\r\n"
                    "Connection: close\r\n\r\n"
                    "<html><head><style>body { background-color: green; } h1 { color: blue; text-align: center; } form { display: flex; justify-content: center; align-items: center; height: 100vh; }</style></head>"
                    "<body>" + response_content + "</body></html>";

    send(client_socket, http_response.c_str(), http_response.length(), 0);
    close(client_socket);
}

int Station::tcp_server(int port) {
    int server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket < 0) {
        perror("Error creating socket");
        return -1;
    }

    int opt = 1;
    setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in server_addr;
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(port);

    if (bind(server_socket, (struct sockaddr *) &server_addr, sizeof(server_addr)) < 0) {
        perror("Error binding socket");
        close(server_socket);
        return -1;
    }

    if (listen(server_socket, MAX_CONNECTIONS) < 0) {
        perror("Error listening on socket");
        close(server_socket);
        return -1;
    }

    fd_set read_fds;
    int max_fd = server_socket;

    std::cout << "TCP server listening on port " << port << std::endl;

    while (true) {
        FD_ZERO(&read_fds);
        FD_SET(server_socket, &read_fds);

        timeval timeout;
        timeout.tv_sec = 10; // 10 seconds timeout
        timeout.tv_usec = 0;

        int activity = select(max_fd + 1, &read_fds, nullptr, nullptr, &timeout);

        if (activity < 0) {
            perror("Error in select");
            continue;
        }

        if (FD_ISSET(server_socket, &read_fds)) {
            sockaddr_in client_addr;
            socklen_t client_len = sizeof(client_addr);
            int client_socket = accept(server_socket, (struct sockaddr *) &client_addr, &client_len);

            if (client_socket < 0) {
                perror("Error accepting connection");
                continue;
            }

            std::cout << "New connection from " << inet_ntoa(client_addr.sin_addr) << ":" << ntohs(client_addr.sin_port)
                      << std::endl;

            handle_request(client_socket);
        }
    }

    close(server_socket);
    return 0;
}

