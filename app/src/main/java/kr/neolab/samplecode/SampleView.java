package kr.neolab.samplecode;

import java.util.ArrayList;
import java.util.Iterator;

import kr.neolab.sdk.graphic.Renderer;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.ink.structure.Stroke;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SampleView extends SurfaceView implements SurfaceHolder.Callback
{
	private SampleThread mSampleThread;

	private boolean normalized = false;

	// draw the strokes
	private ArrayList<Stroke> strokes = new ArrayList<Stroke>();

	private Stroke stroke = null;

	private int sectionId = 0, ownerId = 0, noteId = 0, pageId = 0;

	public float scale = 15, offsetX = 0, offsetY = 0;

	private final int paper_width = 100;
	private final int paper_height = 130;

	private int first_point_x = 0;
	private int first_point_y = 0;

	public SampleView( Context context )
	{
		super( context );

		getHolder().addCallback( this );
		mSampleThread = new SampleThread( this.getHolder(), this );
	}
	public void clearView(){
		strokes.clear();
		invalidate();
	}
	public ArrayList<ArrayList<Integer>> processStrokes() {
		//find bounding box
		float min_x=Float.MAX_VALUE,max_x=Float.MIN_VALUE,min_y=Float.MAX_VALUE,max_y=Float.MIN_VALUE;
		for (Stroke ori_stroke : strokes){
			for (int i=0;i<ori_stroke.size();i++){
				final Dot ori_dot = ori_stroke.get(i);
				min_x = min(min_x,ori_dot.x);
				max_x = max(max_x,ori_dot.x);
				min_y = min(min_y,ori_dot.y);
				max_y = max(max_y,ori_dot.y);
			}
		}
		float strokeOffsetX = min_x;
		float strokeOffsetY = min_y;

		//normalize strokes
		ArrayList<ArrayList<Integer>> processedStrokes = new ArrayList<ArrayList<Integer>>();
		for (Stroke ori_stroke : strokes){
			ArrayList<Integer> new_stroke = new ArrayList<Integer>();
			for (int i=0;i<ori_stroke.size();i++){
				final Dot ori_dot = ori_stroke.get(i);
				new_stroke.add((int)(100.0 * (ori_dot.x-strokeOffsetX)));
				new_stroke.add((int)(100.0 * (ori_dot.y-strokeOffsetY)));
			}
			processedStrokes.add(new_stroke);
		}

		return processedStrokes;
	}
	public ArrayList<Stroke> getStrokes(){
		return strokes;
	}
	public void normalizeStrokes(){
		if(normalized){
			return;
		}

		/*
		//find bounding box
		float min_x=Float.MAX_VALUE,max_x=Float.MIN_VALUE,min_y=Float.MAX_VALUE,max_y=Float.MIN_VALUE;
		for (Stroke new_stroke : strokes){
			for (int i=0;i<new_stroke.size();i++){
				Dot new_dot = new_stroke.get(i);
				min_x = min(min_x,new_dot.x);
				max_x = max(max_x,new_dot.x);
				min_y = min(min_y,new_dot.y);
				max_y = max(max_y,new_dot.y);
			}
		}
		final float stroke_area_width = max_x-min_x;
		final float stroke_area_height = max_y-min_y;
		final float area_rate = (float)(stroke_area_width*scale)/(float)(getWidth());
		float rescale = 3.2f;
		if (area_rate*rescale >= 0.8f){
			rescale = 0.8f/area_rate;
		}

		float center_x = (min_x+max_x)/2 * rescale;
		float center_y = (min_y+max_y)/2 * rescale;

		float strokeOffsetX = paper_width/2 - center_x;
		float strokeOffsetY = paper_height/2 - center_y;

		//normalize strokes
		for (Stroke new_stroke : strokes){
			for (int i=0;i<new_stroke.size();i++){
				Dot new_dot = new_stroke.get(i);
				new_dot.x *=  rescale;
				new_dot.y *=  rescale;
				new_dot.x += strokeOffsetX;
				new_dot.y += strokeOffsetY;
			}
		}
		*/

		normalized = true;
	}

	/*
	public void setPageSize(int canvas_width,int canvas_height)
	{
		final float width_scale = (float)(canvas_width) / (float)(paper_width);
		final float height_scale = (float)(canvas_height) / (float)(paper_height);
		scale = min(width_scale,height_scale);

		final float paper_ratio = (float)paper_width/(float)paper_height;
		final float canvas_ratio = (float)canvas_width/(float)canvas_height;
		offsetX = 0;
		offsetY = 0;
		if (paper_ratio > canvas_ratio){
			final float paper_scale = (float)canvas_width/(float)paper_width;
			offsetY = ((canvas_height - paper_height*paper_scale)/2);
		}else{
			final float paper_scale = (float)canvas_height/(float)paper_height;
			offsetX = ((canvas_width - paper_width*paper_scale)/2);
		}

		scale *= 8.0f;
		offsetX = 150;
		offsetY = 150;
	}
	*/
	public void setRenderParams(float scale)
	{
		this.scale = scale;
		this.offsetX = 150;
		this.offsetY = 150;
	}

	@Override
	public void draw( Canvas canvas )
	{
		canvas.drawColor( Color.LTGRAY );

		if (strokes != null && strokes.size() > 0 )
		{
			Renderer.draw( canvas, strokes.toArray( new Stroke[0] ), scale, offsetX, offsetY );
		}
	}

	@Override
	public void surfaceChanged( SurfaceHolder arg0, int arg1, int arg2, int arg3 )
	{
	}

	@Override
	public void surfaceCreated( SurfaceHolder arg0 )
	{
		mSampleThread = new SampleThread( getHolder(), this );
		mSampleThread.setRunning( true );
		mSampleThread.start();
	}

	@Override
	public void surfaceDestroyed( SurfaceHolder arg0 )
	{
		mSampleThread.setRunning( false );

		boolean retry = true;

		while ( retry )
		{
			try
			{
				mSampleThread.join();
				retry = false;
			}
			catch ( InterruptedException e )
			{
				e.getStackTrace();
			}
		}
	}

	public void addDot( int sectionId, int ownerId, int noteId, int pageId, int x, int y, int fx, int fy, int force, long timestamp, int type, int color )
	{
		if (normalized){
			clearView();
			normalized = false;
		}

		if (strokes.isEmpty()){
			first_point_x = x;
			first_point_y = y;
		}
		x = x - first_point_x;
		y = y - first_point_y;

		if ( this.sectionId != sectionId || this.ownerId != ownerId || this.noteId != noteId || this.pageId != pageId )
		{
			strokes = new ArrayList<Stroke>();

			this.sectionId = sectionId;
			this.ownerId = ownerId;
			this.noteId = noteId;
			this.pageId = pageId;
		}

		if ( DotType.isPenActionDown( type ) || stroke == null || stroke.isReadOnly() )
		{
			stroke = new Stroke( sectionId, ownerId, noteId, pageId, color );
			strokes.add( stroke );
		}

		stroke.add( new Dot( x, y, fx, fy, force, type, timestamp ) );
	}

	public void addStrokes( Stroke[] strs )
	{
		if (normalized){
			clearView();
			normalized = false;
		}
		for ( Stroke stroke : strs )
		{
			strokes.add( stroke );
		}
	}

	public class SampleThread extends Thread
	{
		private SurfaceHolder surfaceholder;
		private SampleView mSampleiView;
		private boolean running = false;

		public SampleThread( SurfaceHolder surfaceholder, SampleView mView )
		{
			this.surfaceholder = surfaceholder;
			this.mSampleiView = mView;
		}

		public void setRunning( boolean run )
		{
			running = run;
		}

		@Override
		public void run()
		{
			setName( "SampleThread" );

			Canvas mCanvas;

			while ( running )
			{
				mCanvas = null;

				try
				{
					mCanvas = surfaceholder.lockCanvas(); // lock canvas

					synchronized ( surfaceholder )
					{
						if ( mCanvas != null )
						{
							mSampleiView.draw( mCanvas );
						}
					}
				}
				finally
				{
					if ( mCanvas != null )
					{
						surfaceholder.unlockCanvasAndPost( mCanvas ); // unlock
																		// canvas
					}
				}
			}
		}
	}
}
