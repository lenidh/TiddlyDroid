package de.lenidh.tiddlydroid

import android.net.Uri
import android.support.v4.util.ArraySet
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.HashMap

/**
 * A Collection of [WikiDescriptor] objects with unique URIs.
 */
class WikiDescriptors : Iterable<WikiDescriptor> {
    private val data = HashMap<Uri, WikiDescriptor>()

    /**
     * Gets the number of [WikiDescriptor]s in this collection.
     */
    val size: Int get() = data.size

    /**
     * Returns an iterator over the elements of this collection.
     */
    override fun iterator(): Iterator<WikiDescriptor> {
        return data.values.iterator()
    }

    /**
     * Adds the specified [WikiDescriptor] to this collection.
     *
     * If the collection already contains a descriptor for the same URI, the specified value will
     * replace that descriptor.
     *
     * @return The descriptor replaced by the specified value or `null` if no descriptor was
     *         replaced.
     */
    fun put(descriptor: WikiDescriptor): WikiDescriptor? {
        return data.put(descriptor.uri, descriptor)
    }

    /**
     * Returns the [WikiDescriptor] for the specified URI.
     *
     * @return The descriptor for the specified URI, or `null` if no descriptor for the specified
     *         URI is found.
     */
    operator fun get(uri: Uri): WikiDescriptor? {
        return data[uri]
    }

    /**
     * Removes the [WikiDescriptor] for the specified URI.
     *
     * @return The removed descriptor or `null` if no descriptor for the specified URI was found.
     */
    fun remove(uri: Uri): WikiDescriptor? {
        return data.remove(uri)
    }

    /**
     * Returns a string set representation of this collection.
     *
     * Each [WikiDescriptor] object in this collection is serialized into a string that becomes one
     * entry in the returned set.
     *
     * @see fromStringSet
     */
    fun toStringSet(): Set<String> {
        val result = ArraySet<String>(size)
        data.forEach({ entry ->
            val byteStream = ByteArrayOutputStream()
            ObjectOutputStream(byteStream).use { it.writeObject(entry.value) }
            val base64 = Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT)
            result.add(base64)
        })
        return result
    }

    companion object {
        /**
         * Returns a [WikiDescriptor] collection generated from a string set returned by
         * [toStringSet].
         */
        fun fromStringSet(set: Set<String>): WikiDescriptors {
            val result = WikiDescriptors()
            set.forEach({ str ->
                val bytes = Base64.decode(str, Base64.DEFAULT)
                val obj = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
                if(obj is WikiDescriptor) { result.put(obj) }
            })
            return result
        }
    }
}
