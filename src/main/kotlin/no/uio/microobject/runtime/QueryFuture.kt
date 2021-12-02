package no.uio.microobject.runtime

import no.uio.microobject.data.LiteralExpr

class QueryFuture(val query : String, val anchors : MutableMap<String, LiteralExpr>) {
}