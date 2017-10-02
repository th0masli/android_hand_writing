package kr.neolab.samplecode;

/**
 * Created by allen on 2016/12/15.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * @ClassName: MatrixImageView
 * @Description:  ImageView with zooming and moving
 * @author LinJ
 * @date 2015-1-7 11:15:07 am
 *
 */
public class MatrixImageView extends ImageView{
    private final static String TAG="MatrixImageView";
    private GestureDetector mGestureDetector;
    private MatrixTouchListener mListener;
    /**  Matrix template，for initializing */
    private  Matrix mMatrix=new Matrix();
    /**  Length */
    private float mImageWidth;
    /**  Height */
    private float mImageHeight;

    public MatrixImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mListener=new MatrixTouchListener();
        setOnTouchListener(mListener);
        mGestureDetector=new GestureDetector(getContext(), new GestureListener(mListener));
        setAdjustViewBounds(true);
        //Set background color
        setBackgroundColor(Color.WHITE);
        //Set zoom type to FIT_CENTER
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        //mListener.reSetMatrix();
        // TODO Auto-generated method stub
        super.setImageBitmap(bm);
        //get image coordinator transform matrix
        mMatrix.set(getImageMatrix());
        float[] values=new float[9];
        mMatrix.getValues(values);
        mImageWidth=getWidth()/values[Matrix.MSCALE_X];
        mImageHeight=(getHeight()-values[Matrix.MTRANS_Y]*2)/values[Matrix.MSCALE_Y];
    }

    public class MatrixTouchListener implements OnTouchListener{
        /** dragging mode */
        private static final int MODE_DRAG = 1;
        /** zoom mode */
        private static final int MODE_ZOOM = 2;
        /** unsupported */
        private static final int MODE_UNABLE=3;
        /** max zoom */
        float mMaxScale=100;
        /** double clikc zoom scale */
        float mDobleClickScale=2;
        private int mMode = 0;//
        /** start finger distance */
        private float mStartDis;
        /** current matrix */
        private Matrix mCurrentMatrix = new Matrix();

        /** record start point */
        private PointF startPoint = new PointF();
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    //set drag mode
                    mMode=MODE_DRAG;
                    startPoint.set(event.getX(), event.getY());
                    isMatrixEnable();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    //reSetMatrix();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mMode == MODE_ZOOM) {
                        setZoomMatrix(event);
                    }else if (mMode==MODE_DRAG) {
                        setDragMatrix(event);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if(mMode==MODE_UNABLE) return true;
                    mMode=MODE_ZOOM;
                    mStartDis = distance(event);
                    break;
                default:
                    break;
            }

            return mGestureDetector.onTouchEvent(event);
        }

        public void setDragMatrix(MotionEvent event) {
            {
                float dx = event.getX() - startPoint.x; // get x moving distance
                float dy = event.getY() - startPoint.y; // get y moving distance
                //avoiding conflict with double click
                if(Math.sqrt(dx*dx+dy*dy)>10f){
                    startPoint.set(event.getX(), event.getY());
                    mCurrentMatrix.set(getImageMatrix());
                    float[] values=new float[9];
                    mCurrentMatrix.getValues(values);
                    //dx=checkDxBound(values,dx);
                    //dy=checkDyBound(values,dy);
                    mCurrentMatrix.postTranslate(dx, dy);
                    setImageMatrix(mCurrentMatrix);
                }
            }
        }

        /**
         *  See if scale level changed
         *  @return   true is origin value,false represents not origin value
         */
        private boolean isZoomChanged() {
            float[] values=new float[9];
            getImageMatrix().getValues(values);
            //get current x scale level
            float scale=values[Matrix.MSCALE_X];
            //compare with template scale level
            mMatrix.getValues(values);
            return scale!=values[Matrix.MSCALE_X];
        }

        /**
         *  keep image in the boundary
         *  @param values
         *  @param dy
         *  @return
         */
        private float checkDyBound(float[] values, float dy) {
            /*
            float height=getHeight();
            if(mImageHeight*values[Matrix.MSCALE_Y]<height)
                return 0;
            if(values[Matrix.MTRANS_Y]+dy>0)
                dy=-values[Matrix.MTRANS_Y];
            else if(values[Matrix.MTRANS_Y]+dy<-(mImageHeight*values[Matrix.MSCALE_Y]-height))
                dy=-(mImageHeight*values[Matrix.MSCALE_Y]-height)-values[Matrix.MTRANS_Y];
            return dy;
            */
            return dy;
        }

        /**
         *  keep image in the boundary
         *  @param values
         *  @param dx
         *  @return
         */
        private float checkDxBound(float[] values,float dx) {
            /*
            float width=getWidth();
            if(mImageWidth*values[Matrix.MSCALE_X]<width)
                return 0;
            if(values[Matrix.MTRANS_X]+dx>0)
                dx=-values[Matrix.MTRANS_X];
            else if(values[Matrix.MTRANS_X]+dx<-(mImageWidth*values[Matrix.MSCALE_X]-width))
                dx=-(mImageWidth*values[Matrix.MSCALE_X]-width)-values[Matrix.MTRANS_X];
            return dx;
            */
            return dx;
        }
        /**
         *  check scale
         *  @param scale
         *  @param values
         *  @return
         */
        private float checkMaxScale(float scale, float[] values) {
            if(scale*values[Matrix.MSCALE_X]>mMaxScale)
                scale=mMaxScale/values[Matrix.MSCALE_X];
            mCurrentMatrix.postScale(scale, scale,getWidth()/2,getHeight()/2);
            return scale;
        }

        /**
         *  check if reset needed
         *  @return  reset when scale level smaller than template's
         */
        private boolean checkRest() {
            /*
            // TODO Auto-generated method stub
            float[] values=new float[9];
            getImageMatrix().getValues(values);
            //get current x scale level
            float scale=values[Matrix.MSCALE_X];
            //compare with template scale level
            mMatrix.getValues(values);
            return scale<values[Matrix.MSCALE_X];
            */
            return false;
        }

        /**
         *  set scale matrix
         *  @param event
         */
        private void setZoomMatrix(MotionEvent event) {
            //execute when 2 points touched
            if(event.getPointerCount()<2) return;
            float endDis = distance(event);// end distance
            if (endDis > 1f) { // end distance must larger than 10
                float scale = endDis / mStartDis;// get scale param
                mStartDis=endDis;//重置距离
                mCurrentMatrix.set(getImageMatrix());// reinitialize Matrix
                float[] values=new float[9];
                mCurrentMatrix.getValues(values);

                scale = checkMaxScale(scale, values);
                setImageMatrix(mCurrentMatrix);
            }
        }


        /**
         *   reset Matrix
         */
        private void reSetMatrix() {
            mCurrentMatrix.set(mMatrix);
            setImageMatrix(mCurrentMatrix);
        }

        /**
         *  check if support Matrix
         */
        private void isMatrixEnable() {
            if(getScaleType()!=ScaleType.CENTER){
                setScaleType(ScaleType.MATRIX);
            }else {
                mMode=MODE_UNABLE;
            }
        }

        /**
         *  compute distance between 2 fingers
         *  @param event
         *  @return
         */
        private float distance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        /**
         *   trigger when double clicked
         */
        public void onDoubleClick(){
            float scale=isZoomChanged()?1:mDobleClickScale;
            mCurrentMatrix.set(mMatrix);// reinitialize Matrix
            mCurrentMatrix.postScale(scale, scale,getWidth()/2,getHeight()/2);
            setImageMatrix(mCurrentMatrix);
        }
    }


    private class  GestureListener extends SimpleOnGestureListener{
        private final MatrixTouchListener listener;
        public GestureListener(MatrixTouchListener listener) {
            this.listener=listener;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            //get down event
            return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //trigger double click event
            listener.onDoubleClick();
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // TODO Auto-generated method stub
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            // TODO Auto-generated method stub

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // TODO Auto-generated method stub
            super.onShowPress(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onSingleTapConfirmed(e);
        }

    }


}