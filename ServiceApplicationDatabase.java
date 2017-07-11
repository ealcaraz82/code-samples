package com.xxxxxxxx;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ServiceApplicationDatabase extends Service{

	private InspectionDBOpenHelper mInspectionDBOpenHelper;
  
	private Context mAppContext;
	
	private static final String BASE_REPORT_NUMBER = "BASE_REPORT_NUMBER";
	private static final String HIGHEST_REPORT_NUMBER = "HIGHEST_REPORT_NUMBER";
	private static final String REPORT_NUMBER_MANAGEMENT = "REPORT_NUMBER_MANAGEMENT";
	
	//The name and column index of each column in the database divided by table
	//*****************************************************************************
	//STAND ALONE TABLES
		//*****************************************************************************
		//ONE TO MANY
		//*****************************************************************************
			//AGENTS
			private static final String KEY_AGENT_INDEX_COLUMN =
				"AGENT_INDEX_COLUMN"; //KEY
			private static final String KEY_AGENT_NAME_COLUMN =
				"AGENT_NAME_COLUMN";
			private static final String KEY_AGENT_NUMBER_COLUMN =
				"AGENT_NUMBER_COLUMN";
			private static final String KEY_AGENT_EMAIL_COLUMN =
				"AGENT_EMAIL_COLUMN";
			private static final String KEY_AGENT_COMPANY_COLUMN =
				"AGENT_COMPANY_COLUMN";
			
			//*****************************************************************************
			//CLIENTS
			private static final String KEY_CLIENT_INDEX_COLUMN =
				"CLIENT_INDEX_COLUMN"; //KEY
			private static final String KEY_CLIENT_NAME_COLUMN =
				"CLIENT_NAME_COLUMN";
			private static final String KEY_CLIENT_ADDRESS_COLUMN =
				"CLIENT_ADDRESS_COLUMN";
			private static final String KEY_CLIENT_CITY_COLUMN =
				"CLIENT_CITY_COLUMN";
			private static final String KEY_CLIENT_STATE_COLUMN =
				"CLIENT_STATE_COLUMN";
			private static final String KEY_CLIENT_ZIP_COLUMN =
				"CLIENT_ZIP_COLUMN";
			private static final String KEY_CLIENT_NUMBER_COLUMN =
				"CLIENT_NUMBER_COLUMN";
			private static final String KEY_CLIENT_EMAIL_COLUMN =
				"CLIENT_EMAIL_COLUMN";
			
			//*****************************************************************************
			//FEESAVAILABLE
			private static final String KEY_FEEAVAILABLE_INDEX_COLUMN =
				"FEEAVAILABLE_INDEX_COLUMN";  //KEY
			private static final String KEY_FEEAVAILABLE_CATEGORY_COLUMN =
				"FEEAVAILABLE_CATEGORY_COLUMN";
			private static final String KEY_FEEAVAILABLE_COST_COLUMN =
				"FEEAVAILABLE_COST_COLUMN";
			private static final String KEY_FEEAVAILABLE_IS_DEFAULT_COLUMN =
				"FEEAVAILABLE_IS_DEFAULT_COLUMN";
			
			//*****************************************************************************
			//FEESAPPLIED
			private static final String KEY_FEEAPPLIED_INDEX_COLUMN =
				"FEEAPPLIED_INDEX_COLUMN";  //KEY
			private static final String KEY_FEEAPPLIED_REPORT_ID_COLUMN =
				"FEEAPPLIED_REPORT_ID_COLUMN";
			private static final String KEY_FEEAPPLIED_CATEGORY_COLUMN =
				"FEEAPPLIED_CATEGORY_COLUMN";
			private static final String KEY_FEEAPPLIED_COST_COLUMN =
				"FEEAPPLIED_COST_COLUMN";
			
			//*****************************************************************************
			//WEATHERCONDITIONS
			private static final String KEY_WEATHERCONDITION_INDEX_COLUMN =
				"WEATHERCONDITION_INDEX_COLUMN";  //KEY
			private static final String KEY_WEATHERCONDITION_CONDITION_COLUMN =
				"WEATHERCONDITION_CONDITION_COLUMN";
			
		..............................................................................................................
	
	//**************************************************************************************************************************************
	//DATABASE INITIALIZATION, DESTRUCTION, AND MANAGEMENT
	public ServiceApplicationDatabase(){
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		setAppContext(getApplicationContext());
		
		Thread thread = new Thread(null, doBackgroundThreadProcessing,
        "ApplicationDatabase");
		thread.start();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		mInspectionDBOpenHelper.close();
		stopSelf();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		return Service.START_STICKY;
	}
	
	//Runnable that executes the background processing method.
	private final Runnable doBackgroundThreadProcessing = new Runnable() {
	   public void run() {
		   startDatabaseInBackground();
	   }
	};
	
	private void startDatabaseInBackground(){
		//begin background thread and set up any initialization needed before opening the database
		mInspectionDBOpenHelper = new InspectionDBOpenHelper(getApplicationContext(),InspectionDBOpenHelper.DATABASE_NAME,
				null,InspectionDBOpenHelper.DATABASE_VERSION);
	}
	
	//service binding functions
	private final IBinder binder = new MyBinder();
	
	public class MyBinder extends Binder {
	    ServiceApplicationDatabase getService() {
	      return ServiceApplicationDatabase.this;
	    }
	  }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	//external helper functions
	public boolean isOpen(){
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
		return db.isOpen();
	}
	
	public boolean isReportNumberAvailable(int ReportID){
		
		SQLiteDatabase db = mInspectionDBOpenHelper.getWritableDatabase();
		
	    String[] result_columns = new String[] {KEY_INSPECTION_REPORT_ID_COLUMN}; 
	    String where = KEY_INSPECTION_REPORT_ID_COLUMN + "=" + ReportID;
	    Cursor cursor = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, where,
	                             null, null, null, null);

		boolean available =  cursor.getCount() == 0;

		cursor.close();

		return available;
	}

	public int nextAvailableReportNumber(){
		int nextAvailable = 0;
		
		//read stored values
		SharedPreferences sharedPrefs = mAppContext.getSharedPreferences(REPORT_NUMBER_MANAGEMENT, Activity.MODE_PRIVATE);
		int baseNumber = sharedPrefs.getInt(BASE_REPORT_NUMBER, 100);
		int highestReportNumber = sharedPrefs.getInt(HIGHEST_REPORT_NUMBER, 100);
		
		//get total number of inspections in database
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
		String[] result_columns = new String[] {
				KEY_INSPECTION_REPORT_ID_COLUMN};
	    Cursor cursorInspections = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, null,
	                             null, null, null, null);
	    int totalInspections = cursorInspections.getCount();

        cursorInspections.close();
		
	    //if there are no inspections saved then start at the baseNumber
	    if (totalInspections == 0){
	    	return baseNumber;
	    }
	    
	    //if there are no gaps in the inspection numbers, return the next highest 
	    if((totalInspections - 1 + baseNumber) == highestReportNumber){
	    	nextAvailable = highestReportNumber + 1;
	    	return nextAvailable;
	    }
	    
	    //iterate through and find a gap
		for (int i = baseNumber ; i < baseNumber + highestReportNumber ; i++){
			if(isReportNumberAvailable(i)){
				return i;
			}
		}
		
		//error
		return -1;
	}
	
	public boolean isAppointmentAvailable(long requestedDate){
		//get the default inspection length from preferences
		SharedPreferences sharedPrefs = getSharedPreferences(ActivityMain.FIRST_RUN, Activity.MODE_PRIVATE);
		int inspectionLength = sharedPrefs.getInt(ActivityMain.DEFAULT_INSPECTION_LENGTH_IN_MINUTES, 60*3);
		
		//get a handle to the database
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
		
		//calculate the end date/time based on the provided date/time and the default length of the inspection
		//add length to the requested date to get the end date
		Calendar requestedEndDate = Calendar.getInstance();
		requestedEndDate.setTimeInMillis(requestedDate);
		requestedEndDate.roll(Calendar.MINUTE, inspectionLength);
		
		//scheduled for the requested date and time. 
		String[] result_columns = new String[] {
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN, 
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_END_COLUMN};
		
		String where = KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN + " >= " + requestedDate +
				" AND " + KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_END_COLUMN + " <= " +requestedEndDate.getTimeInMillis();
		
	    Cursor cursorInspection = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, where,
	                             null, null, null, null);
		
        boolean conflicted = cursorInspection.getCount() == -1 || cursorInspection.getCount() == 0;

        cursorInspection.close();

		return conflicted;
		
	}
	
	public boolean isAppointmentAvailable(long requestedDate, long requestedEndDate ){

		//get a handle to the database
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
		
		//calculate the end date/time based on the provided dates/times

		//scheduled for the requested date and time. 
		String[] result_columns = new String[] {
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN, 
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_END_COLUMN};
		
		String where = KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN + " >= " + requestedDate +
				" AND " + KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_END_COLUMN + " <= " +requestedEndDate;
		
	    Cursor cursorInspection = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, where,
	                             null, null, null, null);
		
		boolean conflicted = cursorInspection.getCount() == 0;

        cursorInspection.close();

		return conflicted;
		
	}
	
	public ArrayList<CInspectionAppointment> getScheduledInspectionsInDateRange(long BeginningDate, long EndingDate){
		//array to return
		ArrayList<CInspectionAppointment> inspectionAppointments = new ArrayList<>();
		
		//get a handle to the database
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();

		//need report numbers, dates scheduled, and property location indexes from
		//the Inspections table
		String[] result_columns = new String[] {
				KEY_INSPECTION_REPORT_ID_COLUMN,
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN, 
				KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN};
		
		//the date range criteria
		String where = KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN + " >= " + BeginningDate +
				" AND " + KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_END_COLUMN + " <= " + EndingDate;
		
		//retrieve inspections in date range
	    Cursor cursorInspection = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, where,
	                             null, null, null, null);
	    
	    //set up the cursor
	    cursorInspection.moveToFirst();
	    
	    //create appointment objects and add them to the returning array
	    for (int i=0 ; i<cursorInspection.getCount(); i++){
	    	
	    	//get report id
	    	int reportID = cursorInspection.getInt(cursorInspection.getColumnIndex(KEY_INSPECTION_REPORT_ID_COLUMN));
	    	
	    	//create inspection appointment
	    	CInspectionAppointment appointment = 
	    			new CInspectionAppointment(
	    					reportID,
	    					//retrieve the client(s) name
	    					getInspectionClient(reportID).toString(),
	    					//retrieve the property address
	    					getPropertyLocation(cursorInspection.getInt(cursorInspection.getColumnIndex(KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN))).toString(),
	    					//date scheduled
	    					cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN))
	    					);
	    	
	    	//add inspection appointment to the result array
	    	inspectionAppointments.add(appointment);
	    	
	    	//increment the cursor
	    	cursorInspection.moveToNext();
	    	
	    }

        cursorInspection.close();
	    
		return inspectionAppointments;
		
	}
	
	//*********************************************************************
	//INSPECTION

	..............................................................................................................
	
	public CInspection getInspection(int ReportNumber){
		//retrieves an inspection by report number
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
	    
		String[] result_columns = new String[] {
				KEY_INSPECTION_DATE_SETUP_BEGAN_COLUMN, 
				KEY_INSPECTION_DATE_SETUP_MODIFIED_COLUMN, 
				KEY_INSPECTION_DATE_SETUP_COMPLETE_COLUMN, 
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN, 
				KEY_INSPECTION_IS_SETUP_COMPLETE_COLUMN, 
				KEY_INSPECTION_DATE_INSPECTION_BEGAN_COLUMN,
				KEY_INSPECTION_DATE_INSPECTION_ENDED_COLUMN, 
				KEY_INSPECTION_IS_INSPECTION_COMPLETE_COLUMN, 
				KEY_INSPECTION_INSPECTOR_INDEX_COLUMN, 
				KEY_INSPECTION_COMPANY_INFO_INDEX_COLUMN, 
				KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN, 
				KEY_INSPECTION_BILLING_TAX_PERCENTAGE_COLUMN,
				KEY_INSPECTION_BEDROOM_COUNT_COLUMN, 
				KEY_INSPECTION_BATHROOM_COUNT_COLUMN, 
				KEY_INSPECTION_SQUARE_FOOTAGE_COLUMN, 
				KEY_INSPECTION_UTILITIES_STATUS_COLUMN, 
				KEY_INSPECTION_STRUCTURE_AGE_YEARS_COLUMN,
				KEY_INSPECTION_STRUCTURE_AGE_MONTHS_COLUMN,
				KEY_INSPECTION_ROOF_AGE_YEARS_COLUMN,
				KEY_INSPECTION_ROOF_AGE_MONTHS_COLUMN,
				KEY_INSPECTION_BUILDING_TYPE_INDEX_COLUMN, 
				KEY_INSPECTION_STATE_OF_OCCUPANCY_INDEX_COLUMN,
				KEY_INSPECTION_ADDITIONS_COLUMN,
				KEY_INSPECTION_CONVERSIONS_COLUMN,
				KEY_INSPECTION_FRONT_FACES_INDEX_COLUMN,
				KEY_INSPECTION_TEMPERATURE_COLUMN};
		
		String where = KEY_INSPECTION_REPORT_ID_COLUMN + " = " + ReportNumber;
		
	    Cursor cursorInspection = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, where,
	                             null, null, null, null);
	    
	    cursorInspection.moveToFirst();
	    
		CInspection inspection = new CInspection(mAppContext);
		
		if (toBoolean(cursorInspection.getInt(cursorInspection.getColumnIndex(KEY_INSPECTION_IS_SETUP_COMPLETE_COLUMN)))){
			//copy all items
			inspection.setReportNumber(ReportNumber);
			
			inspection.setDateSetupBegan(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_SETUP_BEGAN_COLUMN)));
			inspection.setDateSetupModified(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_SETUP_MODIFIED_COLUMN)));
			inspection.setDateSetupComplete(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_SETUP_COMPLETE_COLUMN)));
			inspection.setDateInspectionScheduled(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN)));
			inspection.setDateInspectionBegan(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_INSPECTION_BEGAN_COLUMN)));
			inspection.setDateInspectionCompleted(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_INSPECTION_ENDED_COLUMN)));
			
			inspection.setInspectorIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_INSPECTOR_INDEX_COLUMN)));
			inspection.setCompanyInfoIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_COMPANY_INFO_INDEX_COLUMN)));
			inspection.setPropertyLocationIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN)));
			inspection.setBillingTaxPercentage(cursorInspection.getFloat(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BILLING_TAX_PERCENTAGE_COLUMN)));
			inspection.setAdditions(cursorInspection.getString(
					cursorInspection.getColumnIndex(KEY_INSPECTION_ADDITIONS_COLUMN)));
			inspection.setConversions(cursorInspection.getString(
					cursorInspection.getColumnIndex(KEY_INSPECTION_CONVERSIONS_COLUMN)));
			inspection.setFrontFaces(cursorInspection.getString(
					cursorInspection.getColumnIndex(KEY_INSPECTION_FRONT_FACES_INDEX_COLUMN)));
			inspection.setTemperature(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_TEMPERATURE_COLUMN)));
			inspection.setBathroomCount(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BATHROOM_COUNT_COLUMN)));
			inspection.setBedroomCount(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BEDROOM_COUNT_COLUMN)));
			inspection.setBuildingTypeIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BUILDING_TYPE_INDEX_COLUMN)));
			inspection.setRoofAgeYears(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_ROOF_AGE_YEARS_COLUMN)));
			inspection.setSquareFootage(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_SQUARE_FOOTAGE_COLUMN)));
			inspection.setStateOfOccupancyIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_STATE_OF_OCCUPANCY_INDEX_COLUMN)));
			inspection.setStructureAgeYears(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_STRUCTURE_AGE_YEARS_COLUMN)));
			inspection.setUtilitiesOn(toBoolean(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_UTILITIES_STATUS_COLUMN))));
			
		}else{
			//only copy items that are part of InspectionSetup
			inspection.setReportNumber(ReportNumber);
			
			inspection.setDateSetupBegan(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_SETUP_BEGAN_COLUMN)));
			inspection.setDateSetupModified(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_SETUP_MODIFIED_COLUMN)));
			inspection.setDateSetupComplete(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_SETUP_COMPLETE_COLUMN)));
			inspection.setDateInspectionScheduled(cursorInspection.getLong(cursorInspection.getColumnIndex(KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN)));
			
			inspection.setPropertyLocationIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN)));
			
			inspection.setBathroomCount(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BATHROOM_COUNT_COLUMN)));
			inspection.setBedroomCount(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BEDROOM_COUNT_COLUMN)));
			inspection.setBuildingTypeIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_BUILDING_TYPE_INDEX_COLUMN)));
			inspection.setRoofAgeYears(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_ROOF_AGE_YEARS_COLUMN)));
			inspection.setSquareFootage(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_SQUARE_FOOTAGE_COLUMN)));
			inspection.setStateOfOccupancyIndex(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_STATE_OF_OCCUPANCY_INDEX_COLUMN)));
			inspection.setStructureAgeYears(cursorInspection.getShort(
					cursorInspection.getColumnIndex(KEY_INSPECTION_STRUCTURE_AGE_YEARS_COLUMN)));
			inspection.setUtilitiesOn(toBoolean(cursorInspection.getInt(
					cursorInspection.getColumnIndex(KEY_INSPECTION_UTILITIES_STATUS_COLUMN))));
			
		}
		cursorInspection.close();
		
		db.close();
		
		return inspection;
	}
	
	public ArrayList<CInspectionSetupSummary> getUnfinishedInspectionSetups(){
		//returns a list of all unfinished inspection setups in an array
		ArrayList<CInspectionSetupSummary> inspectionSummaries = new ArrayList<>();
		
		//get handle for database
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
	    
		//set result columns
		String[] result_columns = new String[] {
				KEY_INSPECTION_REPORT_ID_COLUMN,
				KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN,
				KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN};
		
		//set search criteria
		String where = KEY_INSPECTION_IS_SETUP_COMPLETE_COLUMN + " = " + 1237;
		
		//get cursor with results
	    Cursor cursorUnfinishedInspectionsInfo = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTION, 
	                             result_columns, where,
	                             null, null, null, null);
	    
	    //move cursor to first item
	    cursorUnfinishedInspectionsInfo.moveToFirst();
	    
	    //iterate and extract all summaries
	    for (int i = 0 ; i < cursorUnfinishedInspectionsInfo.getCount() ; i++){
	    	//holder for summary
	    	CInspectionSetupSummary newSummary = new CInspectionSetupSummary();
	    	
	    	//get the report number for the stored summary
	    	newSummary.setReportNumber(cursorUnfinishedInspectionsInfo.getInt(
		    		cursorUnfinishedInspectionsInfo.getColumnIndex(KEY_INSPECTION_REPORT_ID_COLUMN)));
	    	
	    	//get the date the inspection is scheduled
			newSummary.setDateScheduled(cursorUnfinishedInspectionsInfo.getLong(
					cursorUnfinishedInspectionsInfo.getColumnIndex(KEY_INSPECTION_DATE_INSPECTION_SCHEDULED_BEGIN_COLUMN)));
		
			//get the client for the inspection
			CClient client = getInspectionClient(newSummary.getReportNumber());
			
			//check to see if there is a client specified
			if(client != null){
				//there is a client specified so get the name
				newSummary.setClient(client.getName());
			}
			
			//reinitialize database
			db = mInspectionDBOpenHelper.getReadableDatabase();
			
			//set the result columns for the property location
			result_columns = new String[] {
					KEY_PROPERTYLOCATION_ADDRESS_COLUMN, 
					KEY_PROPERTYLOCATION_CITY_COLUMN, 
					KEY_PROPERTYLOCATION_STATE_COLUMN, 
					KEY_PROPERTYLOCATION_ZIP_COLUMN};
			
			//set the search criteria for the property location
			where = KEY_PROPERTYLOCATION_INDEX_COLUMN + " = " + cursorUnfinishedInspectionsInfo.getInt(
					cursorUnfinishedInspectionsInfo.getColumnIndex(KEY_INSPECTION_PROPERTY_LOCATION_INDEX_COLUMN));
			
			//get the results for the property locations
		    Cursor cursorPropertyLocationInfo = db.query(InspectionDBOpenHelper.DATABASE_TABLE_PROPERTYLOCATIONS, 
		                             result_columns, where,
		                             null, null, null, null);
		    
		    //set the cursor to first item
		    cursorPropertyLocationInfo.moveToFirst();
		    
		    //instantiate the location variable
		    CPropertyLocation location = new CPropertyLocation();
		    
		    //check to see if there is a property location
		    if(cursorPropertyLocationInfo.getCount() != 0){
			    //copy info about location
			    location.setPropertyAddress(cursorPropertyLocationInfo.getString(
			    		cursorPropertyLocationInfo.getColumnIndex(KEY_PROPERTYLOCATION_ADDRESS_COLUMN)));
			    location.setPropertyCity(cursorPropertyLocationInfo.getString(
			    		cursorPropertyLocationInfo.getColumnIndex(KEY_PROPERTYLOCATION_CITY_COLUMN)));
			    location.setPropertyState(cursorPropertyLocationInfo.getString(
			    		cursorPropertyLocationInfo.getColumnIndex(KEY_PROPERTYLOCATION_STATE_COLUMN)));
			    location.setPropertyZip(cursorPropertyLocationInfo.getString(
			    		cursorPropertyLocationInfo.getColumnIndex(KEY_PROPERTYLOCATION_ZIP_COLUMN)));
		    
			    //copy the location as a string
			    newSummary.setPropertyLocation(location.toString());
		    }
		    
		    //close the property info results
		    cursorPropertyLocationInfo.close();
		    
		    //add the summary to the set
		    inspectionSummaries.add(newSummary);
		    
		    //go to the next summary
		    cursorUnfinishedInspectionsInfo.moveToNext();
	    	
	    }
	    
	    //close the list of unfinished inspections
	    cursorUnfinishedInspectionsInfo.close();
	    
	    //close the database
	    db.close();
		
	    //return the results
		return inspectionSummaries;
	}
	
	..............................................................................................................
	
	public ArrayList<CDisclaimer> getInspectionDisclaimers(int ReportID){
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
	    
		ArrayList<Integer> inspectionDisclaimerIndexes = new ArrayList<>();
		
		String[] result_columns = new String[] {
				KEY_INSPECTIONDISCLAIMER_DISCLAIMER_INDEX_COLUMN};
		
		String where = KEY_INSPECTIONDISCLAIMER_REPORT_ID_COLUMN + " = " + ReportID;
		
	    Cursor cursorInspectionDisclaimerIndexesList = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONDISCLAIMERS, 
	                             result_columns, where,
	                             null, null, null, null);
	    
	    cursorInspectionDisclaimerIndexesList.moveToFirst();
	    
	    for (int i = 0 ; i < cursorInspectionDisclaimerIndexesList.getCount() ; i++){
	    	
	    	inspectionDisclaimerIndexes.add(cursorInspectionDisclaimerIndexesList.getInt(
	    			cursorInspectionDisclaimerIndexesList.getColumnIndex(KEY_INSPECTIONDISCLAIMER_DISCLAIMER_INDEX_COLUMN)));
	    	
	    	cursorInspectionDisclaimerIndexesList.moveToNext();
	    }
	    
	    cursorInspectionDisclaimerIndexesList.close();
	    
	    ArrayList<CDisclaimer> disclaimers = new ArrayList<>();
	    
	    for ( int i = 0 ; i < inspectionDisclaimerIndexes.size() ; i++){
	    	disclaimers.add(getDisclaimer(inspectionDisclaimerIndexes.get(i)));
	    }
	    
	    db.close();
		
		return disclaimers;
	}
	
	public long removeInspectionDisclaimers(int ReportID){
		String where = KEY_INSPECTIONDISCLAIMER_REPORT_ID_COLUMN + "=" + ReportID;
		  
	    // Delete the rows that match the where clause.
	    SQLiteDatabase db = mInspectionDBOpenHelper.getWritableDatabase();
	    int result = db.delete(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONDISCLAIMERS, where, null);
	    
	    db.close();
	    
	    return result;
	}
	
	public long removeInspectionDisclaimers(int ReportID, int DisclaimerIndex){
		String where = KEY_INSPECTIONDISCLAIMER_REPORT_ID_COLUMN + " = " + ReportID + " AND " +
		KEY_INSPECTIONDISCLAIMER_DISCLAIMER_INDEX_COLUMN + " = " + DisclaimerIndex;
		  
	    // Delete the rows that match the where clause.
	    SQLiteDatabase db = mInspectionDBOpenHelper.getWritableDatabase();
	    int result = db.delete(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONDISCLAIMERS, where, null);
	    
	    db.close();
	    
	    this.removeDisclaimer(DisclaimerIndex);
	    
	    return result;
	}

	..............................................................................................................
	
	//*********************************************************************
	//INSPECTIONCLIENTS
	
	public long addInspectionClient(int ReportID, int ClientIndex){
		ContentValues newValues = new ContentValues();
		
		newValues.put(KEY_INSPECTIONCLIENT_REPORT_ID_COLUMN, ReportID);
		newValues.put(KEY_INSPECTIONCLIENT_CLIENT_INDEX_COLUMN, ClientIndex);
		
		SQLiteDatabase db = mInspectionDBOpenHelper.getWritableDatabase();
		long result = db.insert(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONCLIENTS, 
				null, newValues);
		
		db.close();
		
		return result;
	}
	
	public CClient getInspectionClient(int ReportID){
		
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
		
		String[] result_columns = new String[] {
				KEY_INSPECTIONCLIENT_CLIENT_INDEX_COLUMN};
		
		String where = KEY_INSPECTIONCLIENT_REPORT_ID_COLUMN + " = " + ReportID;
		
	    Cursor cursorInspectionClientIndex = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONCLIENTS, 
	                             result_columns, where,
	                             null, null, null, null);
	    
	    cursorInspectionClientIndex.moveToFirst();
	    
	    int clientIndex = -1;
	    
	    CClient newClient = null;
	    
	    if(cursorInspectionClientIndex.getCount() != 0){
	    	clientIndex = cursorInspectionClientIndex.getInt(
	    			cursorInspectionClientIndex.getColumnIndex(KEY_INSPECTIONCLIENT_CLIENT_INDEX_COLUMN));
	    
	    	newClient = this.getClientInfo(clientIndex);
	    	
	    }
    	
	    cursorInspectionClientIndex.close();
	    
	    db.close();
	    
	    return newClient;
	}
	
	public long removeInspectionClient(int ReportID, int ClientIndex){
		
		String where = KEY_INSPECTIONCLIENT_REPORT_ID_COLUMN + " = " + ReportID +
		" AND " + KEY_INSPECTIONCLIENT_CLIENT_INDEX_COLUMN + " = " + ClientIndex;

		// Delete the rows that match the where clause.
		SQLiteDatabase db = mInspectionDBOpenHelper.getWritableDatabase();
		int result = db.delete(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONCLIENTS, where, null);
		
		db.close();
		
		return result;
	}

	..............................................................................................................
	
	public ArrayList<CPersonPresent> getAllInspectionPresentFor(int ReportID){
		ArrayList<CPersonPresent> PeoplePresent = new ArrayList<>();
		
		SQLiteDatabase db = mInspectionDBOpenHelper.getReadableDatabase();
		
		String[] result_columns = new String[] {
				KEY_INSPECTIONPRESENTFOR_PERSON_TYPE_COLUMN,
				KEY_INSPECTIONPRESENTFOR_PERSON_INDEX_COLUMN};
		
		String where = KEY_INSPECTIONPRESENTFOR_REPORT_ID_COLUMN + " = " + ReportID;
		
	    Cursor cursorReviewPresentForIndexesList = db.query(InspectionDBOpenHelper.DATABASE_TABLE_INSPECTIONPRESENTFOR, 
	                             result_columns, where,
	                             null, null, null, null);
	    
	    cursorReviewPresentForIndexesList.moveToFirst();
	    
	    for (int i = 0 ; i < cursorReviewPresentForIndexesList.getCount() ; i++){
	    	CPersonPresent person = new CPersonPresent();
	    	
	    	person.setPersonType(cursorReviewPresentForIndexesList.getInt(
	    			cursorReviewPresentForIndexesList.getColumnIndex
	    			(KEY_INSPECTIONPRESENTFOR_PERSON_TYPE_COLUMN)));
	    	
	    	person.setPersonIndex(cursorReviewPresentForIndexesList.getInt(
	    			cursorReviewPresentForIndexesList.getColumnIndex
	    			(KEY_INSPECTIONPRESENTFOR_PERSON_INDEX_COLUMN)));
	    	
	    	person.setReportID(ReportID);

	    	cursorReviewPresentForIndexesList.moveToNext();
	    	
	    	PeoplePresent.add(person);
	    	
	    }
	    	
	    cursorReviewPresentForIndexesList.close();
	    
	    
	    for (int i = 0 ; i < PeoplePresent.size() ; i++){
	    	
	    	switch(PeoplePresent.get(i).getPersonType()){
		    	case 0:{ //Client 
		    		
		    		String[] result_columns1 = new String[] {
		    				KEY_CLIENT_NAME_COLUMN};
		    		
		    		String where1 = KEY_CLIENT_INDEX_COLUMN + " = " + PeoplePresent.get(i).getPersonIndex();
		    		
		    	    Cursor cursorClientsPresentList = db.query(InspectionDBOpenHelper.DATABASE_TABLE_CLIENTS, 
		    	                             result_columns1, where1,
		    	                             null, null, null, null);
		    	    
		    	    cursorClientsPresentList.moveToFirst();
		    		
		    	    PeoplePresent.get(i).setPersonName(cursorClientsPresentList.getString(
		    				cursorClientsPresentList.getColumnIndex(
			    					KEY_CLIENT_NAME_COLUMN)));
		    		
		    		cursorClientsPresentList.close();
		    		
		    		break;
		    	}
		    	
		    	case 1:{ //CAgent
		    		
		    		String[] result_columns1 = new String[] {
		    				KEY_AGENT_NAME_COLUMN};
		    		
		    		String where1 = KEY_AGENT_INDEX_COLUMN + " = " + PeoplePresent.get(i).getPersonIndex();
		    		
		    	    Cursor cursorAgentsPresentList = db.query(InspectionDBOpenHelper.DATABASE_TABLE_AGENTS, 
		    	                             result_columns1, where1,
		    	                             null, null, null, null);
		    	    
		    	    cursorAgentsPresentList.moveToFirst();
		    		
		    	    PeoplePresent.get(i).setPersonName(cursorAgentsPresentList.getString(
		    				cursorAgentsPresentList.getColumnIndex(
		    						KEY_AGENT_NAME_COLUMN)));
		    		
		    		cursorAgentsPresentList.close();
		    		
		    		break;
		    	}
	    	}
	    }
    	
	    db.close();
		
		return PeoplePresent;
		
	}
	
	..............................................................................................................
	
	//*********************************************************************
	//HELPER CLASS
	private static class InspectionDBOpenHelper extends SQLiteOpenHelper {
		
		private static final String DATABASE_NAME = "inspectionDatabase.db";
		private static final int DATABASE_VERSION = 1;
		
		//tables
		private static final String DATABASE_TABLE_AGENTS = "Agents";  //1
		private static final String DATABASE_TABLE_CLIENTS = "Clients";  //2
		private static final String DATABASE_TABLE_FEESAVAILABLE = "FeesAvailable";  //3
		private static final String DATABASE_TABLE_FEESAPPLIED = "FeesApplied";  //4
		private static final String DATABASE_TABLE_WEATHERCONDITIONS = "WeatherConditions";  //5
		..............................................................................................................
		
		// SQL statements to create the database.
		private static final String DATABASE_ADD_TABLE_1 = 
			"CREATE TABLE " + DATABASE_TABLE_AGENTS + " (" + 
			KEY_AGENT_INDEX_COLUMN + " INTEGER PRIMARY KEY, " +
			KEY_AGENT_NAME_COLUMN + " TEXT NOT NULL, " +
			KEY_AGENT_NUMBER_COLUMN + " TEXT NOT NULL, " +
			KEY_AGENT_EMAIL_COLUMN + " TEXT, " +
			KEY_AGENT_COMPANY_COLUMN + " TEXT " + ");";
			
		private static final String DATABASE_ADD_TABLE_2 = 
			"CREATE TABLE " + DATABASE_TABLE_CLIENTS + " (" + 
			KEY_CLIENT_INDEX_COLUMN + " INTEGER PRIMARY KEY, " +
			KEY_CLIENT_NAME_COLUMN  + " TEXT, " +
			KEY_CLIENT_ADDRESS_COLUMN + " TEXT, " +
			KEY_CLIENT_CITY_COLUMN + " TEXT, " +
			KEY_CLIENT_STATE_COLUMN + " TEXT, " +
			KEY_CLIENT_ZIP_COLUMN + " TEXT, " +
			KEY_CLIENT_NUMBER_COLUMN + " TEXT, " +
			KEY_CLIENT_EMAIL_COLUMN + " TEXT " + ");";
			
		private static final String DATABASE_ADD_TABLE_3 = 
			"CREATE TABLE " + DATABASE_TABLE_FEESAVAILABLE + " (" +
			KEY_FEEAVAILABLE_INDEX_COLUMN + " INTEGER PRIMARY KEY, " +
			KEY_FEEAVAILABLE_CATEGORY_COLUMN  + " TEXT NOT NULL, " +
			KEY_FEEAVAILABLE_COST_COLUMN  + " REAL NOT NULL," +
			KEY_FEEAVAILABLE_IS_DEFAULT_COLUMN + " INTEGER DEFAULT '1237' " + ");";
		
		private static final String DATABASE_ADD_TABLE_4 = 
			"CREATE TABLE " + DATABASE_TABLE_FEESAPPLIED + " (" + 
			KEY_FEEAPPLIED_INDEX_COLUMN + " INTEGER PRIMARY KEY, " +
			KEY_FEEAPPLIED_CATEGORY_COLUMN + " TEXT NOT NULL, " +
			KEY_FEEAPPLIED_COST_COLUMN + " REAL NOT NULL, "  + 
			KEY_FEEAPPLIED_REPORT_ID_COLUMN + " INTEGER NOT NULL " + ");";
		
		private static final String DATABASE_ADD_TABLE_5 = 
			"CREATE TABLE " + DATABASE_TABLE_WEATHERCONDITIONS + " (" +
			KEY_WEATHERCONDITION_INDEX_COLUMN + " INTEGER PRIMARY KEY, " +
			KEY_WEATHERCONDITION_CONDITION_COLUMN + " TEXT NOT NULL " + ");";
			
		..............................................................................................................
		
		public InspectionDBOpenHelper(Context context, String name,
	                  CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
	
		// Called when no database exists in disk and the helper class needs
		// to create a new one.
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_ADD_TABLE_1);
			db.execSQL(DATABASE_ADD_TABLE_2);
			db.execSQL(DATABASE_ADD_TABLE_3);
			db.execSQL(DATABASE_ADD_TABLE_4);
			db.execSQL(DATABASE_ADD_TABLE_5);
		..............................................................................................................
		}

	}
	
	//converter from int/boolean to Boolean
	private Boolean toBoolean(int bool){
		//1237
		return bool == 1231;
	}

	
	private void setAppContext(Context mAppContext) {
		this.mAppContext = mAppContext;
	}

	public Context getAppContext() {
		return mAppContext;
	}

}
