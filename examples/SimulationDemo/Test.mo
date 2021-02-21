model Tank 
  "Simple tank model (based on the one in Isolde Dressler's master thesis project)"
  type Flow=Real(unit="l/s");
  type Uniting=Real(unit="l/s");
  
  Flow outFlow;
  parameter Flow inFlow=1;
  input Boolean afterOpen = false;
  output Integer filling;

  protected Real level;
  constant Real A=1;
  constant Real a=0.2;
  constant Real hmax=1;
  constant Real  g=-9.81;
equation 
  der(level) = (inFlow - outFlow)/(hmax*A);
  filling = integer(level*100);
  if afterOpen then
    outFlow = sqrt(max(0,2*g*hmax*level))*a;
  else
    outFlow = 0;
  end if;
end Tank;