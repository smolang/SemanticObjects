block Prey
  input Real x(start = x0) "Predator";
  output Real y(start = y0) "Prey";
  parameter Real x0 = 10;
  parameter Real y0 = 10;
  parameter Real alpha = 0.4;
  parameter Real beta = 0.02;
equation
  der(y) = y*(beta*x - alpha);
end Prey;