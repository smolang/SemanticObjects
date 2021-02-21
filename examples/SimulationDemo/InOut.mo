model Linear 
  input Integer inPort;
  output Integer outPort;
  output Integer leak;

  protected Real level;
equation 
  der(level) = inPort;
  leak = max(0, level - 50);
  outPort = integer(level);
end Linear;
