package microobject.test.type

import microobject.test.MicroObjectTest
import microobject.type.TypeChecker

open class MicroObjectTypeTest  : MicroObjectTest() {

    protected fun checkMet(className : String, metName : String, filename: String) : TypeChecker{
        val pair = initTc(filename, StringLoad.RES)
        val classes = retrieveClass(className, pair.second)
        assert(classes.size == 1)
        val methods = retrieveMethod(metName, classes[0])
        assert(methods.size == 1)
        val met = methods[0]
        pair.first.checkMet(met, className)
        return pair.first
    }

    protected fun checkClass(className : String, filename: String) : TypeChecker{
        val pair = initTc(filename, StringLoad.RES)
        val classes = retrieveClass(className, pair.second)
        assert(classes.size == 1)
        val classDef = classes[0]
        pair.first.checkClass(classDef)
        return pair.first
    }
}