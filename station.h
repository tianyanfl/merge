#pragma once

#include <sys/socket.h>
#include <netinet/in.h>
#include <mutex>
#include <unordered_map>
#include <vector>
#include <memory>
#include "string_util.h"
#include "time_util.h"
#include "lib/threadpool/ThreadPool.h"

extern const std::string STATION_NAME_REQUEST;
extern const std::string STATION_NAME_RESPONSE;
extern const std::string ROUTE_REQUEST;
extern const std::string ROUTE_RESPONSE;

class UDPRouteDiagram;

class Route;

typedef std::unordered_map<std::string, std::vector<std::shared_ptr<UDPRouteDiagram>>> RouteTable;
typedef std::unordered_map<std::string, std::vector<std::shared_ptr<Route>>> TimeTable;

class Station {
public:
    std::string name;

    ThreadPool pool;

    int http_port;
    int http_service;

    int udp_port;
    int udp_service;

    int context_id;
    std::mutex context_lock;

    RouteTable route_response_dict;
    std::mutex route_response_lock;

    std::vector<std::pair<std::string, int>> neighbors;
    std::unordered_map<std::string, std::pair<std::string, int>> neighbor_dict;
    std::mutex neighbor_lock;

    Station(const Station &) = delete;

    void operator=(const Station &) = delete;

    Station(const std::string &name, int http_port, int udp_port,
            const std::vector<std::pair<std::string, int>> &neighbors);

    void start();

//    void start_http_service();
//
//    void handle_http(int conn);

    bool check_the_station(const std::string &station);

    std::string decode_url(const std::string &input);

    void generate_station_list(std::string &response);

    void handle_request(int client_socket);

    int tcp_server(int port);

    void start_udp_service();

    void handle_udp(const std::string &request, const std::pair<std::string, int> &address);

    void start_station_name_service();

    std::unique_ptr<UDPRouteDiagram> request_route(UDPRouteDiagram &from_diagram, TimeTable &time_table);

    TimeTable get_time_table();

    static std::shared_ptr<Route> get_route(TimeTable &time_table, const std::string &destination, int minute);

//    static std::pair<std::string, int> parse_http_request(const std::string& data);
//
//    static std::string create_http_response(const std::string& body);

    static sockaddr_in addrpair_to_sockaddr(const std::pair<std::string, int> &addr);

    static std::pair<std::string, int> sockaddr_to_addrpair(const sockaddr_in &addr);
};

class Route {
public:
    const static int TIME_NONE;

    int departure_time;
    std::string departure_station;
    std::string route_name;
    std::string departing_from;
    int arrival_time;
    std::string arrival_station;

    Route(const Route &) = delete;

    void operator=(const Route &) = delete;

    Route(std::string departure_time, std::string departure_station, std::string route_name, std::string departing_from,
          std::string arrival_time, std::string arrival_station);

    std::string to_string();
};

class UDPRouteDiagram {
public:
    std::string dtype;
    int context_id;
    int minute;
    std::vector<std::string> from_list;
    std::string destination;
    std::vector<std::string> route_list;

    UDPRouteDiagram(const UDPRouteDiagram &) = delete;

    void operator=(const UDPRouteDiagram &) = delete;

    UDPRouteDiagram(const std::string &dtype, int context_id, int minute, const std::vector<std::string> &from_list,
                    const std::string &destination, const std::vector<std::string> &route_list);

    static std::unique_ptr<UDPRouteDiagram> from_str(const std::string &data);

    std::string context_str();

    std::string to_string();
};
