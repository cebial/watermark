package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

var useAlpha = false
var transparencyColor: Color? = null
var markX = 0
var markY = 0

fun error(msg: String) {
    println(msg)
    exitProcess(404)
}

fun loadImage(filename: String, type: String = "image"): BufferedImage {
    val file = File(filename)
    if (!File(filename).exists()) error("The file $filename doesn't exist.")

    val img = ImageIO.read(file)
    if (img.colorModel.numComponents < 3) error("The number of $type color components isn't 3.")
    if (img.colorModel.pixelSize !in listOf(24, 32)) error("The $type isn't 24 or 32-bit.")

    return img
}

fun main() {
    // request source image
    println("Input the image filename:")
    val src = loadImage(readln())

    // request watermark image
    println("Input the watermark image filename:")
    val wm = loadImage(readln(), "watermark")

    // can we fit the watermark?
    if (src.width < wm.width || src.height < wm.height) error("The watermark's dimensions are larger.")

    // request transparency method and color
    if (wm.colorModel.hasAlpha()) {
        println("Do you want to use the watermark's Alpha channel?")
        if (readln() == "yes") useAlpha = true
    } else {
        println("Do you want to set a transparency color?")
        if (readln() == "yes") {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            val input = readln()
            if (!input.matches(Regex("\\d+ \\d+ \\d+"))) error("The transparency color input is invalid.")

            val (r, g, b) = input.split(' ').map { it.toInt() }
            if (r !in 0..255 || g !in 0..255 || b !in 0..255) error("The transparency color input is invalid.")

            transparencyColor = Color(r, g, b)
        }
    }

    // request transparency percentage
    println("Input the watermark transparency percentage (Integer 0-100):")
    val weight = readln().toIntOrNull() ?: -1
    if (weight == -1) error("The transparency percentage isn't an integer number.")
    if (weight !in 0..100) error("The transparency percentage is out of range.")

    // request position method
    println("Choose the position method (single, grid):")
    val method = readln()
    if (!method.matches(Regex("(single|grid)"))) error("The position method input is invalid.")

    // request watermark position if the user only wants one mark
    if (method == "single") {
        println("Input the watermark position ([x 0-${src.width - wm.width}] [y 0-${src.height - wm.height}]):")
        val input = readln()
        if (!input.matches(Regex("-?\\d+ -?\\d+"))) error("The position input is invalid.")

        val (x, y) = input.split(' ').map { it.toInt() }
        if (x !in 0..src.width - wm.width || y !in 0..src.height - wm.height) error("The position input is out of range.")

        markX = x
        markY = y
    }

    // request filename
    println("Input the output image filename (jpg or png extension):")
    val outname = readln()
    if (!outname.matches(Regex(".*\\.(jpg|png)"))) error("The output file extension isn't \"jpg\" or \"png\".")

    // all good to go, let's create our canvas
    val dest = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB)

    // loop through the source image, pixel by pixel
    for (x in 0 until src.width) {
        for (y in 0 until src.height) {
            // we get the base colour from the source
            val i = Color(src.getRGB(x, y))

            // now let's see if we can get a watermark pixel to combine with
            val w = when {
                // for a grid, divide the current x and y position by width and height and use the remainder
                method == "grid" -> Color(wm.getRGB(x % wm.width, y % wm.height), useAlpha)

                // for a single mark, check if the current x and y position is within the watermark boundaries
                x in markX until markX + wm.width && y in markY until markY + wm.height ->
                    Color(wm.getRGB(x - markX, y - markY), useAlpha)

                // otherwise fall back to the colour of the source pixel
                else -> i
            }

            // either use the source colour,
            val color = if (w == i || w.alpha == 0 || w == transparencyColor) i

            // or combine the two colours we found
            else Color(
                (weight * w.red + (100 - weight) * i.red) / 100,
                (weight * w.green + (100 - weight) * i.green) / 100,
                (weight * w.blue + (100 - weight) * i.blue) / 100
            )

            // paint the canvas
            dest.setRGB(x, y, color.rgb)
        }
    }

    // write the final image to disk
    val outfile = File(outname)
    ImageIO.write(dest, outfile.extension, outfile)
    println("The watermarked image $outname has been created.")
}