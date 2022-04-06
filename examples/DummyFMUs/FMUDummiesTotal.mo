package FMUDummies
  /* These FMUs do nothing, but can be loaded for type checking */
  block Clock
    output Real clock;
  equation
    clock = 1;
  end Clock;

  block InnerWall
    input Real t_areaLeft;
    input Real t_areaRight;
    output Real h_wall;
  equation
    h_wall = t_areaLeft + t_areaRight;
  end InnerWall;

  block OuterWall
    input Real t_areaLeft;
    input Real t_areaRight;
    output Real h_wall;
  equation
    h_wall = t_areaLeft + t_areaRight * 0.5;
  end OuterWall;

  block Room
    input Real h_wallLeft;
    input Real h_wallRight;
    input Real h_powerHeater;
    output Real t_room;
  equation
    t_room = h_wallLeft + h_wallRight;
  end Room;

  block NewRoom
    input Real h_wallLeft;
    input Real h_wallRight;
    input Real h_powerHeater;
    output Real t_room;
  equation
    t_room = h_wallLeft + h_wallRight + 1;
  end NewRoom;

  block Controller
    input Real t_roomLeft;
    input Real t_roomRight;
    output Real h_roomLeft;
    output Real h_roomRight;
  equation
    h_roomLeft = t_roomLeft + t_roomRight + 1;
    h_roomRight = t_roomLeft + t_roomRight - 1;
  end Controller;

  /* These FMUs do nothing, but can be loaded for type checking */
end FMUDummies;