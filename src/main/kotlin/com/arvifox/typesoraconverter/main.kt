package com.arvifox.typesoraconverter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File

fun main(args: Array<String>) {
    println("Hello World!")
//    File("//").walkTopDown().forEach {
//        println(it.path)
//    }
    val inputFile = File(args.first())
    val outputFile = File(inputFile.parent, "${args[2]}.json")
    val inputContent = inputFile.readText()
    val write = buildJsonObject {
        put("runtime_id", args[1].toInt())
        buildTop(this, inputContent, args[3].toInt() > 0)
        putJsonArray("versioning") { }
    }
    outputFile.writeText(write.toString())
}

private fun buildTop(builder: JsonObjectBuilder, inputContent: String, addCustom: Boolean) {
    builder.putJsonObject("types") {
        if (addCustom) {
            put("String", "Text")
            put("FixedU128", "u128")
        }
        val tree = Json.parseToJsonElement(inputContent)
        if (tree is JsonObject) {
            buildTypes(this, tree)
        }
    }
}

private fun buildTypes(builder: JsonObjectBuilder, tree: JsonObject) {
    tree.forEach { tag, value ->
        if (value is JsonPrimitive) {
            builder.put(tag, value.content)
        } else if (value is JsonObject) {
            if (value.containsKey("_enum")) {
                val el = value["_enum"]
                if (el is JsonArray) {
                    builder.putJsonObject(tag) {
                        buildEnumItem(this, el)
                    }
                } else if (el is JsonObject) {
                    builder.putJsonObject(tag) {
                        buildEnumMapItem(this, el)
                    }
                }
            } else {
                builder.putJsonObject(tag) {
                    buildStruct(this, value)
                }
            }
        }
    }
}

private fun buildStruct(builder: JsonObjectBuilder, tree: JsonObject) {
    builder.put("type", "struct")
    builder.putJsonArray("type_mapping") {
        tree.forEach { t, u ->
            buildArrayItem(this, t, u)
        }
    }
}

private fun buildArrayItem(builder: JsonArrayBuilder, t: String, v: JsonElement) {
    builder.addJsonArray {
        add(t)
        add(v.jsonPrimitive.content)
    }
}

private fun buildEnumItem(builder: JsonObjectBuilder, a: JsonArray) {
    builder.put("type", "enum")
    builder.putJsonArray("value_list") {
        a.forEach {
            add(it.jsonPrimitive.content)
        }
    }
}

private fun buildEnumMapItem(builder: JsonObjectBuilder, o: JsonObject) {
    fun innerItem(builder: JsonArrayBuilder, t: String, v: JsonElement) {
        builder.addJsonArray {
            add(t)
            add(v.jsonPrimitive.content)
        }
    }
    builder.put("type", "enum")
    builder.putJsonArray("type_mapping") {
        o.forEach { t, u ->
            innerItem(this, t, u)
        }
    }
}