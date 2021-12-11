# -*- encoding: utf-8 -*-

import sys
import time

import pygame

score_file = "./scores.txt"
width = 800
height = 600


def start():
    pygame.init()
    pygame.mixer.init()
    pygame.display.init()
    pygame.key.set_repeat(100, 10)
    screen = pygame.display.set_mode(size=(width, height))
    pygame.display.set_caption("Ball Clip")

    # load images
    game_font_100 = pygame.font.Font('../resource/font.ttf', 100)
    game_font_80 = pygame.font.Font('../resource/font.ttf', 80)
    game_font_60 = pygame.font.Font('../resource/font.ttf', 60)
    background_image = pygame.image.load('../resource/image/background.png').convert_alpha()
    ball_image = pygame.image.load('../resource/image/ball.png').convert_alpha()  # 设置透明通道
    beat_clapper_image = pygame.image.load('../resource/image/beat_clapper.png').convert_alpha()

    # music
    start_music, click_music, end_music, = init_music()
    # score
    scores = read_score()

    # game element
    ball = pygame.transform.scale(ball_image, (50, 50))
    beat_clapper = pygame.transform.scale(beat_clapper_image, (200, 10))

    # location
    ball_location = ball.get_rect()
    ball_speed = [-4, 4]
    beat_clapper_location = beat_clapper.get_rect()
    beat_clapper_location.bottom = height

    start_music.play()

    game_start = False
    game_over = False
    score = 0.0
    move_up = 0
    clock = pygame.time.Clock()
    last_time = time.time()

    while not game_start:
        game_start = start_game(background_image, clock, game_font_100, game_font_80, scores, screen)

    while True:
        now_time = time.time()
        if not game_over and last_time - now_time < -1.0:  # 每隔一秒更新一次速度
            last_time, score = reset_status(ball_speed, beat_clapper_location, move_up, now_time, score)

        beat_clapper_move_check(beat_clapper_location)  # hardly  up

        ball_location = ball_location.move(ball_speed)  # move

        ball_speed, game_over = move_result(ball_location, ball_speed, beat_clapper_location, click_music,
                                            end_music, game_over, score)

        # show score
        score_text_font = game_font_60.render('score: {}'.format(round(score)), True, (255, 0, 0))

        screen.fill((255, 255, 255))
        screen.blit(background_image, (0, 0))
        screen.blit(score_text_font, (width - 400, 10))
        screen.blit(ball, ball_location)
        screen.blit(beat_clapper, beat_clapper_location)
        if game_over:
            game_over_action(game_font_100, game_font_80, screen)

        # flash
        pygame.display.flip()
        clock.tick(60)


def move_result(ball_location, ball_speed, beat_clapper_location, click_music, end_music, game_over, score):
    if ball_location.left < 0 or ball_location.right > width:
        # click the left or right boundary
        ball_speed[0] = -ball_speed[0]
        click_music.play()
    elif ball_location.top < 0:
        # click the top
        ball_speed[1] = -ball_speed[1]
        click_music.play()
    elif ball_location.right > beat_clapper_location.left and \
            ball_location.left < beat_clapper_location.right and \
            ball_location.bottom > beat_clapper_location.top:
        # click the clipper
        click_music.play()
        ball_speed[1] = -ball_speed[1]

    elif ball_location.bottom > beat_clapper_location.top:
        # over, touch the bottom
        if not game_over:
            end_music.play()
            game_over = True
            ball_speed = [0, 0]
            write_score(score)

    return ball_speed, game_over


def beat_clapper_move_check(beat_clapper_location):
    for event in pygame.event.get():
        if event.type == pygame.QUIT or (event.type == pygame.KEYDOWN and event.key == pygame.K_q):
            pygame.quit()
            sys.exit()

        if event.type == pygame.KEYDOWN:
            if event.key == pygame.K_RIGHT:
                # right move
                if beat_clapper_location.right + 10 > width:
                    beat_clapper_location.right = width
                    continue
                beat_clapper_location.right += 10
            elif event.key == pygame.K_LEFT:
                # left move
                if beat_clapper_location.left - 10 < 0:
                    beat_clapper_location.left = 0
                    continue
                beat_clapper_location.left -= 10


def reset_status(ball_speed, beat_clapper_location, move_up, now_time, score):
    # speed up
    ball_speed[0] += 0.2 if ball_speed[0] > 0 else -0.2
    ball_speed[1] += 0.2 if ball_speed[1] > 0 else -0.2
    score += ball_speed[0] if ball_speed[0] > 0 else -ball_speed[0]
    score += ball_speed[1] if ball_speed[1] > 0 else -ball_speed[1]

    # hard, move the clipper up 3 pixels
    beat_clapper_location.top -= 3
    move_up += 3
    if (move_up % 30 == 0):
        beat_clapper_location.top += 30

    return now_time, score


def start_game(background_image, clock, game_font_100, game_font_80, scores, screen):
    check_event()

    screen.fill((255, 255, 255))
    screen.blit(background_image, (0, 0))
    start_game_font = game_font_100.render("Ball Game", True, (0, 0, 0))
    screen.blit(start_game_font, (200, 80))

    start_text_font = game_font_80.render("Play Game", True, (0, 0, 0))
    screen.blit(start_text_font, (100, 250))
    start_game_location = start_text_font.get_rect()

    game_exit_font = game_font_80.render("Exit Game", True, (0, 0, 0))
    screen.blit(game_exit_font, (100, 350))
    game_exit_location = game_exit_font.get_rect()

    top_text_font = pygame.font.Font(None, 50)
    top_text = top_text_font.render("Top 3 Scores", True, (0, 0, 0))
    screen.blit(top_text, (550, 280))

    for i, score in enumerate(scores):
        score_dis = top_text_font.render(str(score), True, (0, 0, 0))
        score_loc = (550, 330 + 50 * i)
        screen.blit(score_dis, score_loc)

    start_game_location.left, start_game_location.top = (100, 250)
    game_exit_location.left, game_exit_location.top = (100, 350)

    if pygame.mouse.get_pressed()[0]:
        location = pygame.mouse.get_pos()
        if start_game_location.left < location[0] < start_game_location.right and \
                start_game_location.top < location[1] < start_game_location.bottom:
            return True
        elif game_exit_location.left < location[0] < game_exit_location.right and \
                game_exit_location.top < location[1] < game_exit_location.bottom:
            pygame.quit()
            sys.exit()

    pygame.display.flip()
    clock.tick(60)

    return False


def check_event():
    for event in pygame.event.get():
        if event.type == pygame.QUIT or (event.type == pygame.KEYDOWN and event.key == pygame.K_q):
            pygame.quit()
            sys.exit()


def game_over_action(game_font_100, game_font_80, screen):
    game_over_font = game_font_100.render("Game Over", True, (0, 0, 0))
    game_over_loc = (200, 80)
    screen.blit(game_over_font, game_over_loc)
    restart = game_font_80.render("Restart Game", True, (0, 0, 0))
    restart_loc = (160, 220)
    screen.blit(restart, restart_loc)
    restart_pos = restart.get_rect()
    game_exit = game_font_80.render("Exit Game", True, (0, 0, 0))
    game_exit_loc = (160, 300)
    screen.blit(game_exit, game_exit_loc)
    game_exit_pos = game_exit.get_rect()

    # 奇怪的位置, 为什么初始时不是返回正确位置
    restart_pos.left, restart_pos.top = restart_loc
    game_exit_pos.left, game_exit_pos.top = game_exit_loc

    if pygame.mouse.get_pressed()[0]:
        pos = pygame.mouse.get_pos()
        if restart_pos.left < pos[0] < restart_pos.right and \
                restart_pos.top < pos[1] < restart_pos.bottom:
            # print('重新开始游戏')
            start()
        elif game_exit_pos.left < pos[0] < game_exit_pos.right and \
                game_exit_pos.top < pos[1] < game_exit_pos.bottom:
            # print('退出游戏')
            pygame.quit()
            sys.exit()


def read_score():
    scores_list = []
    with open(score_file, 'r', encoding='utf-8') as file:
        file_score = file.readlines()
        for each in file_score:
            scores_list.append(int(each))
        scores_list = sorted(scores_list, reverse=True)[: 3]
    return scores_list


def write_score(score):
    with open(score_file, 'a+', encoding='utf-8') as file:
        file.write(str(int(score)) + '\n')


def init_music():
    startMusic = pygame.mixer.Sound('../resource/sound/start.wav')
    knockMusic = pygame.mixer.Sound('../resource/sound/fire.wav')
    knockMusic.set_volume(0.2)
    gameOverMusic = pygame.mixer.Sound('../resource/sound/game_over.wav')
    return startMusic, knockMusic, gameOverMusic


if __name__ == "__main__":
    start()
