package devedroid.opensurveyor;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockFragment;

import devedroid.opensurveyor.data.Marker;
import devedroid.opensurveyor.data.SessionManager.SessionListener;
import devedroid.opensurveyor.data.TextMarker;

public class MapFragment extends SherlockFragment implements SessionListener,
		LocationListener {

	private MapView map;
	private ItemizedIconOverlay<OverlayItem> markersOvl;
	private ItemizedIconOverlay<OverlayItem> cMarkerOvl;
	private OverlayItem cMarker;
	private List<OverlayItem> markers;
	private MainActivity parent;
	private PathOverlay track;
	private MyLocationOverlay myLoc;

	private static final String PREF_CENTER_LAT = "centerlat";
	private static final String PREF_CENTER_LON = "centerlon";
	private static final String PREF_ZOOM = "zoom";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);
		parent = (MainActivity) a;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		View root = inflater.inflate(R.layout.frag_map, container, false);

		Button btAdd = (Button) root.findViewById(R.id.btAddSmth);
		btAdd.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				parent.addMarker(new TextMarker(map.getMapCenter(), "preved"));
			}
		});
		Button btFreehand = (Button) root.findViewById(R.id.bt_free_hand);
		btFreehand.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// map.setClickable( !map.isClickable() );
				// Utils.toast(parent, "Clicked button");
				// Utils.logw("MapFragment", "set clickable="+map.isClickable()
				// );
				if (v.getTag() == null) {
					map.getOverlays().add(new FreehandOverlay(parent));
					v.setTag(Boolean.TRUE);
				} else {
					Overlay owl = null;
					for (Overlay o : map.getOverlays())
						if (o instanceof FreehandOverlay)
							owl = o;
					if (owl != null)
						map.getOverlays().remove(owl);
					v.setTag(null);
				}
			}
		});

		map = (MapView) root.findViewById(R.id.mapview);
		map.setClickable(false);
		map.setTileSource(TileSourceFactory.MAPNIK);
		map.setBuiltInZoomControls(true);
		// map.setMinZoomLevel(16);
		// map.setMaxZoomLevel(16);
		map.getController().setZoom(19);
		map.getController().setCenter(new GeoPoint(55.0, 83.0));
		markers = new ArrayList<OverlayItem>();
		markersOvl = new ItemizedIconOverlay<OverlayItem>(parent, markers,
				new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

					@Override
					public boolean onItemSingleTapUp(final int index,
							final OverlayItem item) {
						Utils.toast(parent, "Clicked item " + item);
						return false;// true;
					}

					@Override
					public boolean onItemLongPress(final int index,
							final OverlayItem item) {
						return false;
					}
				});
		map.getOverlays().add(markersOvl);

		// cMarkerOvl = new ItemizedIconOverlay<OverlayItem>(parent, markers,
		// new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
		//
		// @Override
		// public boolean onItemSingleTapUp(final int index,
		// final OverlayItem item) {
		// Utils.toast(parent, "Clicked item "+item);
		// return false;//true;
		// }
		//
		// @Override
		// public boolean onItemLongPress(final int index,
		// final OverlayItem item) {
		// return false;
		// }
		// });
		// map.getOverlays().add(new FreehandOverlay(parent));

		track = new PathOverlay(Color.GREEN, parent);
		map.getOverlays().add(track);

		myLoc = new MyLocationOverlay(parent, map);
		map.getOverlays().add(myLoc);

		map.getOverlays().add(new ScaleBarOverlay(parent));

		map.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Utils.logw("mapfragment", "touch: " + event);
				return false;
			}
		});

		return root;
	}

	class FreehandOverlay extends Overlay {
		// List<List> paths
		private List<List<IGeoPoint>> paths = new ArrayList<List<IGeoPoint>>();
		private List<IGeoPoint> path;
		private Paint paint;

		public FreehandOverlay(Context ctx) {
			super(ctx);
			paint = new Paint();
			paint.setColor(Color.BLUE);
			paint.setStrokeWidth(4);
			paint.setStyle(Style.STROKE);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapview) {

			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					path = new ArrayList<IGeoPoint>();
					paths.add(path);
				case MotionEvent.ACTION_MOVE:
					IGeoPoint p = map.getProjection().fromPixels(
							(int) event.getX(), (int) event.getY());
					path.add(p);
					map.invalidate();
					break;
				case MotionEvent.ACTION_UP:
					optimizePath(path);
					map.invalidate();
					break;
			}

			return true;
		}

		@Override
		protected void draw(Canvas arg0, MapView map, boolean arg2) {
			if (path == null)
				return;
			Point pt = new Point();
			for (List<IGeoPoint> path : paths) {
				Path pth = new Path();
				boolean first = true;
				for (IGeoPoint gpt : path) {
					pt = map.getProjection().toMapPixels(gpt, pt);
					if (!first)
						pth.lineTo(pt.x, pt.y);
					else
						pth.moveTo(pt.x, pt.y);
					first = false;
				}
				arg0.drawPath(pth, paint);
			}
		}

		private void optimizePath(List<IGeoPoint> path) {
			List<IGeoPoint> res = ramerDouglasPeucker(path);
			Utils.logi(this, "optimizing path from "+path.size()+" to "+res.size() );
			path.clear();
			path.addAll(res);
		}

		private List<IGeoPoint> sublist(List<IGeoPoint> src, int start, int end) {
			List<IGeoPoint> res = new ArrayList<IGeoPoint>();
			for(int i=start; i<=end; i++) res.add(src.get(i));
			return res;
		}
		private List<IGeoPoint> ramerDouglasPeucker(List<IGeoPoint> points) {
			if(points.size()<3) return points;
			int endIndex = points.size()-1;
			int maxDistIndex = findMaxPerpDistPoint(points);
			if (maxDistIndex > 0) {
				List<IGeoPoint> result1 = ramerDouglasPeucker(
						sublist(points, 0, maxDistIndex));
				List<IGeoPoint> result2 = ramerDouglasPeucker(
						sublist(points, maxDistIndex, endIndex));
				result1.addAll(result2);
				return result1;

			} else {
				List<IGeoPoint> result = new ArrayList<IGeoPoint>();
	            result.add(points.get(0));
	            result.add(points.get(endIndex));
	            return result;
			}
		}

		private int findMaxPerpDistPoint(List<IGeoPoint> points) {
			double maxDistance = 0D;
			int index = 0;
			for (int i = 0; i < points.size()-1; i++) {
				double distance = dist(points.get(0), points.get(points.size()-1), points.get(i));
				if (distance > maxDistance) {
					maxDistance = distance;
					index = i;
				}
			}
			if (maxDistance > 2) {
				return index;
			}
			return -1;
		}
		
		private double dist(IGeoPoint p1, IGeoPoint p2, IGeoPoint pt) {
			Point pp1 = map.getProjection().toPixels(p1, null);
			Point pp2 = map.getProjection().toPixels(p2, null);
			Point ppt = map.getProjection().toPixels(pt, null);
			int a,b,c;
			a = pp2.y-pp1.y;
			b = pp1.x-pp2.x;
			c = -a*pp1.x - b*pp1.y;
			return dist(a,b,c, ppt.x, ppt.y);
		}
		
		private double dist(int a, int b, int c, int x, int y) {
			return Math.abs(a*x+b*y+c) / Math.sqrt(a*a+b*b);
		}
	}

	public void onActivityCreated(Bundle savedState) {
		super.onActivityCreated(savedState);

		Marker lm = null;
		Utils.logd("MapFragment", "parent=" + parent);
		for (Marker m : parent.getMarkers()) {
			onPoiAdded(m);
			if (m.hasLocation())
				lm = m;
		}
		if (lm != null)
			map.getController().animateTo(lm.getLocation().getGeoPoint());
	}

	@Override
	public void onStart() {
		super.onStart();
		SharedPreferences pref = getActivity().getSharedPreferences(
				getActivity().getPackageName(), 0);
		double lat = pref.getFloat(PREF_CENTER_LAT, 0);
		double lon = pref.getFloat(PREF_CENTER_LON, 0);

		map.getController().setCenter(new GeoPoint(lat, lon));
		map.getController().setZoom(pref.getInt(PREF_ZOOM, 2));
	}

	@Override
	public void onResume() {
		super.onResume();
		myLoc.enableMyLocation();
		Hardware hw = parent.getHardwareCaps();
		if (hw.canGPS()) {
			hw.addListener(this);
		}
	}

	@Override
	public void onPause() {
		SharedPreferences pref = getActivity().getSharedPreferences(
				getActivity().getPackageName(), 0);
		Editor ed = pref.edit();
		ed.putFloat(PREF_CENTER_LAT,
				map.getMapCenter().getLatitudeE6() / 1000000f);
		ed.putFloat(PREF_CENTER_LON,
				map.getMapCenter().getLongitudeE6() / 1000000f);
		ed.putInt(PREF_ZOOM, map.getZoomLevel());
		ed.commit();

		myLoc.disableMyLocation();
		parent.getHardwareCaps().removeListener(this);
		super.onPause();
	}

	@Override
	public void onPoiAdded(Marker m) {
		if (!m.hasLocation())
			return;
		// Utils.logi("", "added marker " + m);
		GeoPoint p = m.getLocation().getGeoPoint();
		OverlayItem oo = new OverlayItem(m.toString(),
				m.getDesc(getResources()), p);
		oo.setMarker(getResources().getDrawable(R.drawable.map_marker));
		// markers.add(oo);
		markersOvl.addItem(oo);
		// track.addPoint(p);
		map.invalidate();
	}

	@Override
	public void onPoiRemoved(Marker m) {
	}

	@Override
	public void onSessionStarted() {
	}

	@Override
	public void onSessionFinished() {
	}

	@Override
	public void onLocationChanged(Location location) {
		track.addPoint(new GeoPoint(location));
		map.invalidate();
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

}
