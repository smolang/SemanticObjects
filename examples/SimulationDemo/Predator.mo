block Predator
  input Real y(start = y0) "Prey";
  output Real x(start = x0) "Predator";
  parameter Real x0 = 10;
  parameter Real y0 = 10;
  parameter Real alpha = 0.1;
  parameter Real beta = 0.4;  
equation
  der(x) = x*(alpha-beta*y);
end Predator;