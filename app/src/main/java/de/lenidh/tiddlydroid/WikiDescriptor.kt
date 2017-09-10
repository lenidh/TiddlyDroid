package de.lenidh.tiddlydroid

import android.net.Uri
import java.io.*

/**
 * Metadata for a TiddlyWiki.
 */
class WikiDescriptor(uri: Uri, title: String = "", subtitle: String = "") : Serializable {

    /**
     * Gets the URI reference identifying the represented TiddlyWiki.
     */
    var uri: Uri = uri
        private set

    /**
     * Gets or sets the title of the TiddlyWiki. (derived if not explicitly set)
     */
    var title: String
        get() = if(_title == "") { uri.lastPathSegment } else { _title }
        set(value) { _title = value }
    private var _title: String = title

   /**
    * Gets or sets the subtitle of the TiddlyWiki.
    */
    @Suppress("CanBePrimaryConstructorProperty")
    var subtitle: String = subtitle

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(stream: ObjectInputStream) {
        uri = Uri.parse(stream.readUTF())
        _title = stream.readUTF()
    }

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        stream.writeUTF(uri.toString())
        stream.writeUTF(_title)
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
