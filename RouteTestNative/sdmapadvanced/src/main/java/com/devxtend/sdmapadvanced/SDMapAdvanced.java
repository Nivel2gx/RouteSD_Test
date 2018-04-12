package com.devxtend.sdmapadvanced;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.artech.activities.ActivityHelper;
import com.artech.android.WithPermission;
import com.artech.android.api.GeoLocationAPI;
import com.artech.android.api.LocationHelper;
import com.artech.base.controls.IGxControlRuntime;
import com.artech.base.metadata.ActionDefinition;
import com.artech.base.metadata.ActionParameter;
import com.artech.base.metadata.layout.GridDefinition;
import com.artech.base.metadata.layout.LayoutItemDefinition;
import com.artech.base.model.Entity;
import com.artech.base.model.EntityList;
import com.artech.base.services.Services;
import com.artech.base.utils.GeoFormats;
import com.artech.controllers.ViewData;
import com.artech.controls.grids.GridAdapter;
import com.artech.controls.grids.GridHelper;
import com.artech.controls.maps.GxMapViewDefinition;
import com.artech.controls.maps.common.IGxMapView;
import com.artech.controls.maps.common.MapItemViewHelper;
import com.artech.ui.Coordinator;
import com.devxtend.genexusmodule.R;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import static android.content.Context.SENSOR_SERVICE;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * SD Map Advanced. Contiene lo siguiente:
 * 	- Dibujar una ruta.
 * 	- Apuntar al norte - navigation.
 * 	- Animación del marker.
 */

public class SDMapAdvanced extends MapView implements IGxControlRuntime, IGxMapView, SensorEventListener,LocationListener
		, GoogleMap.CancelableCallback {

	final static String NAME = "SDMapAdvanced";
	private final static String METHOD_ROUTE = "routepoint";
	private final static String METHOD_ANIMATE_MARKER = "AnimateMarker";
	private final static String EVENT_ON_TAP = "OnTap";
	private float currentDegree = 0f;

	private LatLngBounds bounds = null;
	private final Coordinator mCoordinator;
	private final GxMapViewDefinition mDefinition;
	private boolean mIsAnimatedRunning = false;

	private final SensorManager mSensorManager;
	private final Sensor mAccelerometer;
	private final Sensor magnetometer;

	private boolean mIsReady;
	private boolean mOnResumeInvoked;

	private boolean mIsReadyAndDraw;

	private GridHelper mHelper;
	private GridAdapter mAdapter;
	private MapItemViewHelper mItemViewHelper;

	private ViewData mPendingUpdate;
	private CameraUpdate mPendingCameraUpdate;

	private String mName;
	private int tapCount;
	GoogleMap mMap = null;

    //APUNTAR AL NORTE
	float mDeclination;
    float[] mRotationMatrix = new float[9];
	float [] mGeomagneticMatrix =  new float[9];

	float [] mGravity;
	float [] mGeomagnetic;
	float [] mGravity2;
	float [] mGeomagnetic2;
	boolean success;

	LatLng startPointLatLong;
	LatLng endPointLatLong;
	LatLng myLocation;
	private String PropertyGridVariableName;
	String PropertyApiKey;
	private boolean PropertyMyLocation;
	private boolean PropertyAnimateMarker;
	private boolean PropertyAutoRotate;

	private final static int ITEM_VIEW_WIDTH_MARGIN = 20; // dips

	Marker mMarkerInit, mMarkerEnd;
	private final String TAG = "DX-";

	private PolylineOptions polylineOptions, blackPolylineOptions;
	private Polyline blackPolyline, greyPolyLine;
	private List<LatLng> polyLineList;
	boolean Inicio = true;



	public SDMapAdvanced(Context context, Coordinator coordinator, LayoutItemDefinition definition){

		super(context, new GoogleMapOptions() );

		mCoordinator = coordinator;

		mDefinition = new GxMapViewDefinition(context, (GridDefinition)definition); // ACA LIMPIA LOS VALORES; no viene lo del grid
		mSensorManager = (SensorManager)context.getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		onCreate(new Bundle());

		initialize();
	}

	private void initialize()
	{
		mHelper = new GridHelper(this, mCoordinator, mDefinition.getGrid());
		mHelper.setReservedSpace(ITEM_VIEW_WIDTH_MARGIN);
		mAdapter = new GridAdapter(getContext(), mHelper, mDefinition.getGrid());
		mItemViewHelper = new MapItemViewHelper(this);

		PropertyGridVariableName = mDefinition.getItem().getControlInfo().optStringProperty("@SDMapAdvancedCodeAtt");
		PropertyGridVariableName.replace("&", "");   // NO ANDA , no saca el &
		PropertyApiKey = mDefinition.getItem().getControlInfo().optStringProperty("@SDMapAdvancedGoogleDirectionApi");
		PropertyMyLocation = mDefinition.getItem().getControlInfo().optBooleanProperty("@SDMapAdvancedmylocation");
		PropertyAnimateMarker = mDefinition.getItem().getControlInfo().optBooleanProperty("@SDMapAdvancedAnimateMarker");
		PropertyAutoRotate = mDefinition.getItem().getControlInfo().optBooleanProperty("@SDMapAdvancedAutoRotate");


		getMapAsync(new OnMapReadyCallback()
		{
			@Override
			public void onMapReady(final GoogleMap googleMap)
			{
				mMap = googleMap;

				Log.e("UC:", " ON MAP READY");

				LatLng MVD = new LatLng(-34.869339, -56.167432);
				CameraPosition pos = CameraPosition.builder().target(MVD).zoom(10f).build();

				mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 1000, null);

				mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
					@Override
					public void onMapLoaded() {
						if (bounds != null) {
							mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 1000, null);
						}
					}
				});

				//if(PropertyMyLocation)
					setMyLocation();

				if(PropertyAutoRotate)
					startAutoRotate();


				makeMapReady();

			}
		});

	}


	private void makeMapReady()
	{
		mIsReady = true;
		ViewData pendingUpdate = mPendingUpdate;
		mPendingUpdate = null;

		if (pendingUpdate != null)
			update(pendingUpdate);
	}

	@Override
	public void setEnabled(boolean enabled) {

	}

	@Override
	public void setProperty(String name, Object value) {

		if(name.equalsIgnoreCase("AnimateMarker")) {

			PropertyAnimateMarker = Boolean.parseBoolean(value.toString());
			startAnimation();

		}else if(name.equalsIgnoreCase("AutoRotate")){

			if(Boolean.parseBoolean(value.toString())){
				startAutoRotate();
			}else{
				stopAutoRotate();
			}

		}

	}


	@Override
	public Object getProperty(String name) {
		Log.e(TAG, "entra.. " + name);
		return null;
	}

	public void runOnTapEvent() {
		ActionDefinition actionDef = mCoordinator.getControlEventHandler(this, EVENT_ON_TAP);

		for (ActionParameter param : actionDef.getEventParameters()) {
			String paramName = param.getValueDefinition().getName();
			mCoordinator.setValue(paramName, tapCount);
		}

		mCoordinator.runControlEvent(this, EVENT_ON_TAP);
	}

	private final View.OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			tapCount++;
			runOnTapEvent();
		}
	};

	@Override
	public String getMapType() {
		return null;
	}

	@Override
	public void setMapType(String type) {

	}

	@Override
	public void addListener(GridEventsListener listener) {

	}

	public static LatLng stringToLatLng2(String str)
	{
		Pair<Double, Double> coordinates = GeoFormats.parseGeolocation(str);
		if (coordinates != null)
			return new LatLng(coordinates.first, coordinates.second);
		else
			return null;
	}

	////////////////////////////////////////////////////////////////////

	@Override
	public void update(ViewData data)
	{

		EntityList dataList = data.getEntities();

		endPointLatLong = null;
		LatLngBounds.Builder builder = new LatLngBounds.Builder();

		if (dataList!=null)
		{
			for (Entity dato : dataList) {

				String geolocation = dato.getProperty(PropertyGridVariableName).toString();
				LatLng position = stringToLatLng2(geolocation);

				if (Inicio) {

					startPointLatLong = position;
					Inicio = false;

					mMarkerInit = mMap.addMarker(new MarkerOptions().position(position)
							.flat(true)
							.icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxidest)));

				}

				builder.include(position);
				endPointLatLong = position;
			}
		}

		//bounds = builder.build();

		if (mIsReady)
		{
			if (!mOnResumeInvoked)
			{
				mOnResumeInvoked = true;
				startRouting(startPointLatLong,endPointLatLong);
				onResume();
			}
		}
		else
		{
			mPendingUpdate = data;
		}

		Services.Device.postOnUiThreadDelayed(new Runnable() {
			@Override
			public void run() {
				mIsReadyAndDraw = true;
			}
		}, 2000);

	}

	public void runMethod(String method, List<Object> parameters) {
		Log.e("UC:"," 1. onrunMethod " );
		Log.e("UC:",method );
		Log.e("UC:","routepoint" );
		if (METHOD_ROUTE.equals(method)) {

			try {

				String startPoint = (String) parameters.get(0);
				startPointLatLong = new LatLng(Double.valueOf(startPoint.split(",")[1]), Double.valueOf(startPoint.split(",")[2]));
				Log.e("UC:", startPoint);
				String endPoint = (String) parameters.get(1);
				Log.e("UC:", startPoint);
				endPointLatLong = new LatLng(Double.valueOf(endPoint.split(",")[1]), Double.valueOf(endPoint.split(",")[2]));
				startRouting(startPointLatLong, endPointLatLong);

			} catch (Exception e) {
				e.printStackTrace();
				Log.e("UC:", e.toString());
			}
		}else if(METHOD_ANIMATE_MARKER.equals(method)){

			startAnimation();

		}

	}

	public void startRouting(final LatLng startPosition, LatLng endPosition){

		if (mMap != null) {

			String requestUrl = null;
			try {

				requestUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
						"mode=driving&"
						+ "transit_routing_preference=less_driving&"
						+ "origin=" + startPosition.latitude + "," + startPosition.longitude + "&"
						+ "destination=" + endPosition.latitude + "," + endPosition.longitude + "&"
						+ "key=" + PropertyApiKey; //AIzaSyDVcBkbysc5vY7jRS6nFyn6R54XehN2tso"; //getResources().getString(R.string.google_directions_key);


				Log.d(TAG, requestUrl);

				JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
						requestUrl, null,
						new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {

								Log.d(TAG, response + "");

								try {

									JSONArray jsonArray = response.getJSONArray("routes");
									for (int i = 0; i < jsonArray.length(); i++) {

										JSONObject route = jsonArray.getJSONObject(i);
										JSONObject poly = route.getJSONObject("overview_polyline");
										String polyline = poly.getString("points");

										polyLineList = decodePoly(polyline);

										Log.d(TAG, polyLineList + "");
									}

									//Adjusting bounds
									LatLngBounds.Builder builder = new LatLngBounds.Builder();
									for (LatLng latLng : polyLineList) {
										builder.include(latLng);
									}

									LatLngBounds bounds = builder.build();
									CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
									mMap.animateCamera(mCameraUpdate);


									polylineOptions = new PolylineOptions();
									polylineOptions.color(Color.BLUE);
									polylineOptions.width(15);
									polylineOptions.addAll(polyLineList);

									greyPolyLine = mMap.addPolyline(polylineOptions);

									blackPolylineOptions = new PolylineOptions();
									blackPolylineOptions.width(15);
									blackPolylineOptions.color(Color.BLUE);
									blackPolyline = mMap.addPolyline(blackPolylineOptions);

									//Nuevo Matias.
									mMarkerEnd = mMap.addMarker(new MarkerOptions().position(polyLineList.get(polyLineList.size() - 1))
											.flat(true)
											.icon(BitmapDescriptorFactory.fromResource(R.drawable.locator)));

									startAnimation();

								} catch (Exception e) {
									e.printStackTrace();
								}

							}
						}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d(TAG, error + "");
					}
				});

				RequestQueue requestQueue = Volley.newRequestQueue(this.getContext());
				requestQueue.add(jsonObjectRequest);

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			Log.d(TAG,"Map is not ready yet.");
		}
	}

	private final Runnable mRunnableEnableMyLocationLayer = new Runnable()
	{
		@Override
		public void run()
		{
			Log.e(TAG," Ejecuta el run de EnabledLocation");
			//mMap.setMyLocationEnabled(true);

		}
	};

	private void setMyLocation()
	{
		Log.e("UC:"," SetMylocation pide permisos");
		WithPermission.Builder<Void> permisionBuilder;
		permisionBuilder = new WithPermission.Builder<Void>(ActivityHelper.getCurrentActivity())
				.needs(GeoLocationAPI.getRequiredPermissions())
				.setRequestCode(1010)
				.attachToActivityController()
				.onSuccess(new Runnable()
				{
					@Override
					public void run()
					{

						Location location = LocationHelper.getLastKnownLocation();
						if (location != null) {
							myLocation = stringToLatLng2(location.getLatitude() + "," + location.getLongitude());
							//mMap.setMyLocationEnabled(true);
						}
						else
							Toast.makeText(getContext(), R.string.GXM_CouldNotGetLocationInformation, Toast.LENGTH_SHORT).show();
					}
				})
				.onFailure(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getContext(), "No se obtuvieron los permisos.", Toast.LENGTH_SHORT).show();
					}
				});
		permisionBuilder.build().run();
	}

	public void RoutePoint(String startpoint, String endpoint) {
		mName = startpoint;
		//setText(getContext().getString(R.string.welcome_message, name));
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		float bearing = Math.round(event.values[0]);
		Log.d("onSensorChanged", "Heading: " + Float.toString(bearing) + " degrees");

		//if (mIsReadyAndDraw) {
			if (Math.abs(currentDegree - bearing) > 4) {
			//	if (mIsReadyAndDraw)
					updateCamera(bearing);
				currentDegree = bearing;
			} else {
				//Log.e("UC:", " NO CAMBIO mas de 2");
			}
		//}
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {

	}

	private void updateCamera(float bearing) {

		Log.e("UC:"," UPDATECAMERA");

		if (mMap == null) {
			return;
		}

		CameraPosition oldPos = mMap.getCameraPosition();

		CameraPosition pos = CameraPosition.builder(oldPos)
				.bearing(bearing)
				.target(myLocation)
				.build();

		if (mIsAnimatedRunning)
		{
			Log.e("UC:"," IS animatedRUnning") ;

			mMap.stopAnimation();
			mIsAnimatedRunning = true;
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos),200,this);
		}
		else
		{
			Log.e("UC:"," IS NOT animatedRUnning") ;
			mIsAnimatedRunning = true;
			mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos),200,this);
		}

	}

	@Override
	public void onLocationChanged(Location location) {
		Log.e("UC:"," ONLOCATION CHANGED");
			/*GeomagneticField field = new GeomagneticField(
					(float)location.getLatitude(),
					(float)location.getLongitude(),
					(float)location.getAltitude(),
					System.currentTimeMillis()
			);
			// getDeclination returns degrees
			mDeclination = field.getDeclination();
			*/
	}
	@Override
	public void onFinish() {
		Log.e("onFinish:","onFinish:") ;
		mIsAnimatedRunning = false;
	}

	@Override
	public void onCancel() {
		Log.e("onCancel:"," onCancel" ) ;

	}

	/**
	 * Comienza la navegación apuntando al norte.
	 */
	private void startAutoRotate(){
		//	mSensorManager.registerListener(this,mAccelerometer,	SensorManager.SENSOR_DELAY_GAME);
		//	mSensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_GAME);  //SENSOR_STATUS_ACCURACY_LOW);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
	}

	/**
	 * Para la navegación, deja de utilizar los sensores.
	 */
	private void stopAutoRotate(){
		mSensorManager.unregisterListener(this);
	}

	/**
	 * Comienza la animación del marker.
	 */
	private void startAnimation(){

		if(PropertyAnimateMarker) {
			MarkerAnimation markerAnimation = new MarkerAnimation(polyLineList, mMarkerInit);
			markerAnimation.startAnimation();
		}

	}

	/**
	 * Decode polyline.
	 * @param encoded
	 * @return
	 */
	private List<LatLng> decodePoly(String encoded) {
		List<LatLng> poly = new ArrayList<>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng p = new LatLng((((double) lat / 1E5)),
					(((double) lng / 1E5)));
			poly.add(p);
		}

		return poly;
	}

}
