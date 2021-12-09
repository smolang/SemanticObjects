# run with python3. 
# Requires pexpect
import os
import datetime
import time
import pexpect
import sys
import math


# Use pexpect to make a REPL object to run commands on.
def startRepl(background=None):
    startCommand = "java -jar build/libs/MicroObjects-0.2-all.jar -l"
    if background:
        startCommand += " -b " + background

    repl = pexpect.spawn(startCommand)
    # repl.logfile = sys.stdout.buffer
    repl.expect('MO>')
    return repl



# Run and time one single repl commmand
# Return a dict with information about the run: did it time out, what did it return, how long time did it use, etc.
def runOneReplCommand(repl, command):
    print("\n------  running new command: " + command +  "  ------")
    start = time.time()
    repl.sendline(command)
    r = repl.expect([pexpect.TIMEOUT, 'MO>', pexpect.EOF], timeout=300)
    end = time.time()
    elapsed = end-start

    # detect timeouts
    timeout = False
    if r == 0:
        timeout = True

    # detect exceptions
    exception = False
    if "exception" in repl.before.decode('ascii').lower():
        exception = True


    print("Output:", repl.before.decode('ascii')) # This prints output
    print("Time to run command: " + "{:.2f}".format(elapsed))
    result = {"command": command, "response": repl.buffer.decode('ascii'), "runningTime": elapsed, "timeout": timeout, "exception": exception}
    return result





# LMOL evaluation. We compare eager vs lazy for different lengths of chains of classes depending on the next class.
def lazyChain(benchmarkId):
    resultFile = open("eval/result-" + benchmarkId + ".tsv", 'w').close()  # Erase old data from result file.

    # lazyMethod = "loadFirst"  # In the lazy case: do we continue with only the first in the loaded list or all of them.
    lazyMethod = "loadAll"
    # linkCount = "multiple"  # how are data linked in ttl file? columns or multiple links between all?
    linkCount = "single"
    chainLengths = range(10)
    linkCount = "single"
    dataDepth = 200 # How deep we go with instances in the ttl file. This should be fixed for all runs and be independent of chainlength

    # Setups when different scenarios are run
    if len(sys.argv) > 1:
        name = sys.argv[1]
        if name == "scenario1":
            print("Running scenario 1.")
            linkCount = "single"
            chainLengths = range(10,201,10)

        if name == "scenario2":
            print("Running scenario 2.")
            linkCount = "single"
            linkCount = "multiple"
            chainLengths = range(2,20)

    for dataWidth in [2]: # d
        for chainLength in chainLengths: # The chainlengths to use
            for retrieveLength in ["first", "middle", "last"]: # The retrievelength we use. How far is the object to retrieve.
                if retrieveLength == "first":
                    retrieveLengthInt = 1
                if retrieveLength == "middle":
                    retrieveLengthInt = math.ceil(chainLength/2)
                if retrieveLength == "last":
                    retrieveLengthInt = chainLength
                for method in ["lazy", "eager"]: # run both lazy and eager
                    print("\n\n\n===================================================================================")
                    print("===================================================================================")
                    inputParameters = [("chainLength", chainLength), ("retrieveLength", retrieveLength),  ("method", method), ("dataWidth", dataWidth)]
                    print(inputParameters)

                    # Generate temporary .smol file
                    open("eval/tmp.smol", 'w').close()
                    tempSmolFile = open("eval/tmp.smol", "a")
                    # Classes
                    for i in range(chainLength):
                        tempSmolFile.write(f"""class C{i+1} anchor ?o (\n    String id""")
                        if i < chainLength-1:
                            if method == "eager":
                                tempSmolFile.write(f""",\n    link("?o prog:link{i+2} ?c{i+2}.") C{i+2} c{i+2}""")
                            else:
                                tempSmolFile.write(f""",\n    link("?o prog:link{i+2} ?c{i+2}.") QFut<C{i+2}> c{i+2}""")
                        tempSmolFile.write(f"""\n) end retrieve "?o a prog:C{i+1}. ?o prog:id ?id."\n\n""")

                    # main
                    tempSmolFile.write("main")

                    # eager
                    if method == "eager":
                        tempSmolFile.write(f"""\n    List<C1> C1List := load C1();\n    C1 firstC1 := C1List.get(0);\n    C{retrieveLengthInt} retrieveObj := firstC1""")
                        for i in range(retrieveLengthInt-1):
                            tempSmolFile.write(f".c{i+2}")
                        tempSmolFile.write(f""";\n    print("ID(s) of retrieved object(s):");\n    print(retrieveObj.id);\n""")

                    # lazy
                    else:
                        # lazy - load all
                        if lazyMethod == "loadAll":
                            tempSmolFile.write(f"""\n    List<C1> C1List := load C1();\n""")
                            for i in range(retrieveLengthInt-1):
                                tempSmolFile.write(f"""

    C{i+1}List := C{i+1}List.noDuplicates();

    List<C{i+2}> C{i+2}List := null;
    Int indexC{i+1} := 0;
    Int lenC{i+1} := C{i+1}List.length();
    while (indexC{i+1} < lenC{i+1}) do
        C{i+1} objC{i+1} := C{i+1}List.get(indexC{i+1});
        List<C{i+2}> l := load objC{i+1}.c{i+2};
        if (C{i+2}List = null) then C{i+2}List := l;
        else C{i+2}List.append(l);
        end
        indexC{i+1} := indexC{i+1} + 1;
    end
\n""")
                            tempSmolFile.write(f"""

    C{retrieveLengthInt}List := C{retrieveLengthInt}List.noDuplicates();

    Int indexC{retrieveLengthInt} := 0;
    Int lenC{retrieveLengthInt} := C{retrieveLengthInt}List.length();
    print("ID(s) of retrieved object(s):");
    while (indexC{retrieveLengthInt} < lenC{retrieveLengthInt}) do
        C{retrieveLengthInt} objC{retrieveLengthInt} := C{retrieveLengthInt}List.get(indexC{retrieveLengthInt});
        print(objC{retrieveLengthInt}.id);
        indexC{retrieveLengthInt} := indexC{retrieveLengthInt} + 1;
    end
""")


                        # lazy - load only one
                        else:
                            tempSmolFile.write(f"""\n    List<C1> C1List := load C1();\n    C1 firstC1 := C1List.get(0);\n""")
                            for i in range(retrieveLengthInt-1):
                                tempSmolFile.write(f"""\n    List<C{i+2}> C{i+2}List := load firstC{i+1}.c{i+2};\n    C{i+2} firstC{i+2} := C{i+2}List.get(0);\n""")

                            tempSmolFile.write(f"""\n    print("Retrieved object id");\n    print(firstC{retrieveLengthInt}.id);\n""")
                    tempSmolFile.write("end")
                    tempSmolFile.close()



                    # generate temporary ttl-file
                    open("eval/tmp.ttl", 'w').close()
                    tempTtlFile = open("eval/tmp.ttl", "a")
                    for ic in range(dataWidth): #width
                        for cl in range(dataDepth): #depth
                            tempTtlFile.write(f"""prog:i{cl+1}_{ic+1} a prog:C{cl+1}.\n""")
                            if (linkCount == "multiple"):
                                for i in range(dataWidth):
                                    tempTtlFile.write(f"""prog:i{cl+1}_{ic+1} prog:link{cl+2} prog:i{cl+2}_{i+1}.\n""")
                            else:
                                tempTtlFile.write(f"""prog:i{cl+1}_{ic+1} prog:link{cl+2} prog:i{cl+2}_{ic+1}.\n""")
                            tempTtlFile.write(f"""prog:i{cl+1}_{ic+1} prog:id "{cl+1}_{ic+1}".\n\n""")
                    tempTtlFile.close()



                    # Run commands
                    repl = startRepl(background="eval/tmp.ttl")
                    sessionCommands = [
                        {"command": "read eval/tmp.smol", "recordTime": True},
                        {"command": "auto", "recordTime": True},
                    ]
                    sessionResults = runSession(repl, sessionCommands)
                    print("----------------------------------------------------------")
                    print("session results")
                    print(sessionResults)
                    print("----------------------------------------------------------")

                    appendResultsToFile(benchmarkId, inputParameters, sessionResults)





# benchmark id is just a string which gives name to the file
# inputparameters is a list of pairs [(par1, val1), (par2, val2)], where [0] is the name and [1] is the value
# sessionResults is a dict
def appendResultsToFile(benchmarkId, inputParameters, sessionResults):
    fileName = "eval/result-" + benchmarkId + ".tsv"
    resultFile = open(fileName, 'a')
    # If empty, add header
    if (os.stat(fileName).st_size == 0):
        s = ""
        for p in inputParameters:
            s += str(p[0]) + "\t"
        for singleCommandRes in sessionResults["sessionResults"]:
            s+= singleCommandRes["command"] + "\t"
        resultFile.write(s.strip("\t") + "\n")

    s = ""
    for p in inputParameters:
        s += str(p[1]) + "\t"
    for singleCommandRes in sessionResults["sessionResults"]:
        if singleCommandRes.get("recordTime", False):
            if singleCommandRes["timeout"] and singleCommandRes["exception"]:
                s +=  "TIMEOUT/EXCEPTION" + "\t"
            elif singleCommandRes["timeout"]:
                s +=  "TIMEOUT" + "\t"
            elif singleCommandRes["exception"]:
                s +=  "EXCEPTION" + "\t"
            else:
                s +=  str("{:.3f}".format(singleCommandRes["runningTime"])) + "\t"

    resultFile.write(s.strip("\t") + "\n")
    resultFile.close()



# Run a session with multiple commands
def runSession(repl, sessionCommands):
    sessionResults = []
    timeout = False
    exception = False
    for c in sessionCommands:
        commandRes = runOneReplCommand(repl, c["command"])
        # flat out result and make it into one single dict
        commandRes["recordTime"] = c.get("recordTime", False)
        sessionResults.append(commandRes)
        if (commandRes["timeout"]):
            timeout = True
        if (commandRes["exception"]):
            exception = True
    return {"timeout": timeout, "exception": exception, "sessionResults": sessionResults}


def main():
    # Change to correct working dir
    os.chdir(os.path.dirname(os.path.realpath(__file__)))
    os.chdir("..")

    # Decide which evaluation functions to run. Each of them is standalone and should produce its own result file.
    listOfBenchmarks = [
        ("lazyChain", lazyChain)
    ]
    for benchmark in listOfBenchmarks:
        benchmark[1](benchmark[0])

if __name__ == "__main__":
    main()



