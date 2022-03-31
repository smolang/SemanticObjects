package FMUDummies
  /* These FMUs do nothing, but can be loaded for type checking */
  block Clock
    output Real clock;
  equation
    clock = 1;
  end Clock;

  block InnerWall
    input Real r1;
    input Real r2;
    output Real o;
  equation
    o = r1 + r2;
  end InnerWall;

  block OuterWall
    input Real r1;
    input Real r2;
    output Real o;
  equation
    o = r1 + r2 * 0.5;
  end OuterWall;

  block Room
    input Real r1;
    input Real r2;
    output Real o;
  equation
    o = r1 + r2;
  end Room;

  block NewRoom
    input Real r1;
    input Real r2;
    output Real o;
  equation
    o = r1 + r2 + 1;
  end NewRoom;

  block Controller
    input Real r1;
    input Real r2;
    output Real o1;
    output Real o2;
  equation
    o1 = r1 + r2 + 1;
    o2 = r1 + r2 - 1;
  end Controller;

  /* These FMUs do nothing, but can be loaded for type checking */
end FMUDummies;