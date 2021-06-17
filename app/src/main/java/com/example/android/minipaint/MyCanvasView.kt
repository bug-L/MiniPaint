package com.example.android.minipaint

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat

private const val STROKE_WIDTH = 12f    //has to be float

class MyCanvasView(context: Context) : View(context) {

    /*
    Canvas: 2D drawing surface with methods for drawing.
    Canvas can be associated with a view for display.
    Paint holds style and color information.
    Common pattern to working with canvas:
        Create custom view and override onDraw() and onSizeChanged()
        Override onTouchEvent() to respond to user touch
        Use extra bitmap to cache drawings that build over time
     */
    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)   //color to draw with
    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    //set up the paint used for drawing:
    private val paint = Paint().apply {
        color = drawColor
        //Smooths out edges of what is drawn without affecting shape:
        isAntiAlias = true
        //Dithering affects how colors with higher precision than the device are down-sampled
        isDither = true
        style = Paint.Style.STROKE      //Default: FILL
        strokeJoin = Paint.Join.ROUND   //Default: MITER
        strokeCap = Paint.Cap.ROUND     //Default: BUTT
        strokeWidth = STROKE_WIDTH      //default: Hairline-width (really thin)
    }

    //Path of what the user is drawing:
    private var path = Path()

    private var motionTouchEventX = 0f  //these are used to cache the X and Y
    private var motionTouchEventY = 0f  //co-ordinates of the current touch event

    //cache the latest X and Y values:
    private var currentX = 0f
    private var currentY = 0f

    //reduce how much is drawn to improve performance (such as when the fingers are barely moved)
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    //Draw a frame around the screen
    private lateinit var frame: Rect

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        if (::extraBitmap.isInitialized) extraBitmap.recycle() //Prevent memory leak by recycling old bitmap
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)

        //Calculate a rectangular frame around the picture
        val inset = 40
        frame = Rect(inset, inset, width - inset, height - inset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)    //left and top represents pixels for the top left corner
        // Draw the rectangle (frame) around the canvas
        canvas.drawRect(frame, paint)
    }

    //Called when the user first touches the screen:
    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX) //calculate the distance
        val dy = Math.abs(motionTouchEventY - currentY) //that has been moved
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // quadTo() adds a quadratic bezier (segment) from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2)
            path.quadTo(currentX, currentY,
                (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            //Draw the path in the extra bitmap to cache it
            extraCanvas.drawPath(path, paint)
        }
        invalidate()    //forces redrawing of the screen with updated path
    }

    //Called when user lifts the touch
    private fun touchUp() {
        //reset the path so it doesn't get drawn again
        path.reset()
    }

    //onTouchEvent is called whenever the user touches the display
    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }


}
