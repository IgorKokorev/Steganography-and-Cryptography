package cryptography

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImgFileException( s: String? ): Exception(s)

fun main() {
    while (true) {
        println("Task (hide, show, exit):")
        try {
            when (val inp = readln()) {
                "show" -> show()
                "hide" -> hide()
                "exit" -> break
                else -> println("Wrong task: $inp")
            }
        } catch (e: ImgFileException) {
            println(e.message)
        }
    }
    println("Bye!")
}

// get message from image
fun show() {
    println("Input image file:")
    val inpFileName = readln()

    println("Password:")
    val password = readln()


    val file = File(inpFileName)
    val im = getImage(file)

    val hidden = HiddenMessage()
    var isFinished = false
    for (y in 0 until im.height) {
        for (x in 0 until im.width) {
            val color = im.getRGB(x, y)
            isFinished = hidden.addBit(color and 1)
            if (isFinished) break
        }
        if (isFinished) break
    }

    println("Message:")
    println(hidden.getMessage(password))
}

// hides a message into an image
fun hide () {
    println("Input image file:")
    val inpFileName = readln()
    println("Output image file:")
    val outputFileName = readln()

    println("Message to hide:")
    val message = readln()

    println("Password:")
    val password = readln()

    val hidden = HiddenMessage(message, password)

    val file = File(inpFileName)
    val im = getImage(file) // Image

    if ( (im.width * im.height) < hidden.getSize() )
        throw ImgFileException("The input image is not large enough to hold this message.")

    val resImg = BufferedImage(im.width,im.height,im.type) // resulting image

    var bitNum = 0
    for (y in 0 until im.height) {
        for (x in 0 until im.width) {
            val color = im.getRGB(x, y)
            val resColor = if (bitNum < hidden.getSize())
                (color shr 1 shl 1) + hidden.getBit(bitNum)
            else color
            resImg.setRGB(x, y, resColor)
            bitNum++
        }
    }

    try {
        val outFile = File(outputFileName)
        ImageIO.write(resImg,"png", outFile)
    } catch (e: Exception) {
        throw ImgFileException(e.message)
    }
    println("Message saved in $outputFileName image.")
}

// reads an image from the input file and checks it
fun getImage( file: File ): BufferedImage {
    val im: BufferedImage // Image

    try {
        im = ImageIO.read(file)
    } catch (e: Exception) {
        throw ImgFileException(e.message)
    }

    if (im.colorModel.numComponents != 3)
        throw ImgFileException("The number of color components isn't 3.")
    if (im.colorModel.pixelSize != 24)
        throw ImgFileException("The input file isn't 24-bit.")

    return im
}

// class operates with hidden messages.
// array - array of bytes representing characters of a string to hide converted to bytes.
// array ends with 0, 0, 3 (added to the string to hide).
// size - number of BITs hidden.
class HiddenMessage() {
    // default constructor - creating an empty array
    private var array = byteArrayOf() // array of bytes - hidden message
    private var size = 0 // current length of message in bits

    // creating an object from a string
    constructor(str: String, pwd: String): this() {
        val strArr = str.toByteArray(Charsets.UTF_8)
        val pwdArr = pwd.toByteArray(Charsets.UTF_8)
        this.array = encryptStr(strArr, pwdArr) + 0 + 0 + 3
        this.size = this.array.size * 8
    }

    private fun encryptStr(strArr: ByteArray, pwdArr: ByteArray): ByteArray {
        val resArr = Array<Byte>(strArr.size) {i -> 0}
        for (i in strArr.indices) {
            resArr[i] = (strArr[i].toInt() xor pwdArr[i % pwdArr.size].toInt()).toByte()
        }
        return resArr.toByteArray()
    }


    fun getSize(): Int = this.size

    fun getMessage(pwd: String): String {
        val encrArr = this.array.copyOfRange(0, array.size-3)
        val pwdArr = pwd.toByteArray(Charsets.UTF_8)

        return encryptStr(encrArr, pwdArr).toString(Charsets.UTF_8)
    }

    // get bit number n from the message
    fun getBit(n: Int): Int {
        if ( n < 0 || n >= this.size )
            throw ImgFileException("Wrong bit number.")

        val byte: Int = this.array[n/8].toInt()
        val shift = 7 - (n % 8)
        return ((byte shr shift) and 1)
    }

    // add a bit to the end of the message
    // returns true if 0-0-3 bytes sequence found
    fun addBit(b: Int): Boolean {
        if (b < 0 || b > 1) throw ImgFileException("Wrong bit.")
        if (this.size % 8 == 0) {
            this.array += 0
        }
        this.array[this.array.lastIndex] =
            ((this.array.last().toInt() shl 1) or
                    b).toByte()
        this.size++
        if (this.size % 8 == 0 && this.size >= 24) {
            if (this.array[this.array.lastIndex] == 3.toByte() &&
                this.array[this.array.lastIndex-1] == 0.toByte() &&
                this.array[this.array.lastIndex-2] == 0.toByte())
                return true
        }
        return false
    }
}