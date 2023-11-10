package no.uio.microobject.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import org.jline.reader.LineReaderBuilder
import no.uio.microobject.runtime.REPL
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdfconnection.RDFConnectionFactory
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

// test cases only
var testModel : Model? = null

enum class ReasonerMode {
    off, rdfs, owl
}

data class Settings(var verbose : Boolean,      //Verbosity
                    val materialize : Boolean,  //Materialize
                    var outdir : String,        //path of temporary outputs
                    val tripleStore : String,   // url for the triple store database
                    val background : String,    //owl background knowledge
                    val domainPrefix : String,  //prefix used in the domain model (domain:)
                    val progPrefix : String = "https://github.com/Edkamb/SemanticObjects/Program#",    //prefix for the program (prog:)
                    val runPrefix : String  = "https://github.com/Edkamb/SemanticObjects/Run${System.currentTimeMillis()}#",    //prefix for this run (run:)
                    val langPrefix : String = "https://github.com/Edkamb/SemanticObjects#",
                    val extraPrefixes : HashMap<String, String>,
                    val useQueryType : Boolean = false,
                    val reasoner : ReasonerMode = ReasonerMode.owl
                    ){
    private var prefixMapCache: HashMap<String, String>? = null
    fun prefixMap() : HashMap<String, String> {
        if(prefixMapCache != null) return prefixMapCache as HashMap<String, String>
        prefixMapCache = hashMapOf(
            "domain" to domainPrefix,
            "smol" to langPrefix,
            "prog" to progPrefix,
            "run" to runPrefix,
            "owl" to "http://www.w3.org/2002/07/owl#",
            "rdf" to "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "rdfs" to "http://www.w3.org/2000/01/rdf-schema#",
            "xsd" to "http://www.w3.org/2001/XMLSchema#"
        )
        prefixMapCache!!.putAll(extraPrefixes)
        return prefixMapCache as HashMap<String, String>
    }

    fun replaceKnownPrefixes(string: String) : String{
        var res = string.replace("domain:", "$domainPrefix:")
            .replace("prog:", "$progPrefix:")
            .replace("run:", "$runPrefix:")
            .replace("smol:", "$langPrefix:")
        for( (k,v) in extraPrefixes) res = res.replace("$k:", "$v:")
        return res
    }
    fun replaceKnownPrefixesNoColon(string: String) : String{ //For the HermiT parser, BEWARE: requires that the prefixes and in #
        var res= string.replace("domain:", domainPrefix)
            .replace("prog:", progPrefix)
            .replace("run:", runPrefix)
            .replace("smol:", langPrefix)
        for( (k,v) in extraPrefixes) res = res.replace("$k:", v)
        return res
    }
    fun prefixes() : String {
        var res = """@prefix smol: <${langPrefix}> .
           @prefix prog: <${progPrefix}>.
           @prefix domain: <${domainPrefix}>.
           @prefix run: <${runPrefix}> .""".trimIndent()
        for( (k,v) in extraPrefixes) res = "$res\n@prefix $k: <$v>."
        return res + "\n"
    }

    fun getHeader() : String {
        return """
        @prefix owl: <http://www.w3.org/2002/07/owl#> .
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
        """.trimIndent()
    }
}

class Main : CliktCommand() {
    private val mainMode by option().switch(
        "--execute" to "execute", "-e" to "execute",
        "--load" to "repl",       "-l" to "repl",
    ).default("repl")

    private val reasoner by option("--jenaReasoner", "-j", help="Set value of the internally used reasoner to 'off', 'rdfs', or 'owl' (defaut -> 'owl')").default("owl")
    private val tripleStore by option("--sparqlEndpoint", "-s",  help="url for SPARQL endpoint")
    private val back         by option("--back",      "-b",  help="path to a file containing OWL class definitions as background knowledge.").path()
    private val domainPrefix by option("--domain",    "-d",  help="prefix for domain:.").default("https://github.com/Edkamb/SemanticObjects/ontologies/default#")
    private val input        by option("--input",     "-i",  help="path to a .smol file which is loaded on startup.").path()
    private val replay       by option("--replay",    "-r",  help="path to a file containing a series of REPL commands.").path()
    private val outdir       by option("--outdir",    "-o",   help="path to a directory used to create data files.").path().default(Paths.get("").toAbsolutePath())
    private val verbose      by option("--verbose",   "-v",  help="Verbose output.").flag()
    private val materialize  by option("--materialize", "-m",  help="Materialize triples and dump to file.").flag()
    private val queryType    by option("--useQueryType", "-q",  help="Activates the type checker for access").flag()
    private val extra        by option("--prefixes", "-p", help="Extra prefixes, given as a list PREFIX=URI").associate()

    override fun run() {
        org.apache.jena.query.ARQ.init()

        //check that background knowledge exists
        var backgr = ""
        if(back != null){
            assert(tripleStore == null)
            val file = File(back.toString())
            if(file.exists()){
                backgr = file.readText()
            }else println("Could not find file for background knowledge: ${file.path}")
        }

        var tripleStoreUrl = ""
        if (tripleStore != null){
            assert(back == null)

            val url = tripleStore.toString() + "/query"

            // We check if the connection exists by querying the triple store for a single element
            // If the query fails, we exit the program. There might be a more elegant way of doing it
            val conn = RDFConnectionFactory.connect(url)

            val query = QueryFactory.create("SELECT * WHERE { ?s ?p ?o } LIMIT 1")
            val qexec = conn.query(query)
            val result: ResultSet = qexec.execSelect()

            // check that we retrieved something
            if (!result.hasNext()) {
                println("Error: the url for the triple store is not valid.")
                exitProcess(-1)
            } else {
                tripleStoreUrl = tripleStore.toString()
                conn.close()
            }
        }

        val reasonerMode = when(reasoner){
            "off" -> ReasonerMode.off
            "rdfs" -> ReasonerMode.rdfs
            "owl" -> ReasonerMode.owl
            else -> {
                println("Error: the reasoner mode is not valid.")
                exitProcess(-1)
            }
        }

        if (input == null && mainMode != "repl"){
            println("Error: please specify an input .smol file using \"--input\".")
            exitProcess(-1)
        }

        val repl = REPL( Settings(verbose, materialize, outdir.toString(), tripleStoreUrl, backgr, domainPrefix, extraPrefixes=HashMap(extra), useQueryType = queryType, reasoner = reasonerMode))
        if(input != null){
            repl.command("read", input.toString())
        }
        if(replay != null){
            val str = replay.toString()
            File(str).forEachLine {
                if(!it.startsWith("#") && it != "") {
                    println("MO-auto> $it")
                    val splits = it.split(" ", limit = 2)
                    val left = if(splits.size == 1) "" else splits[1]
                    repl.command(splits.first(), left)
                }
            }
        }
        if(mainMode == "repl"){
            println("Interactive shell started.")
            val reader = LineReaderBuilder.builder().build()
            do {
                val next = reader.readLine("MO> ") ?: break
                val splits = next.trim().split(" ", limit = 2)
                val left = if(splits.size == 1) "" else splits[1].trim()
            } while (!repl.command(splits.first(), left))
        }else if(replay == null){
            repl.runAndTerminate() //command("auto", "");
        }
        repl.terminate()
    }
}

fun main(args:Array<String>) = Main().main(args)
