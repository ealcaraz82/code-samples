package com.xxxxxxxxxx;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Objects;

public class ActivityInspectionSetup extends AppCompatActivity
	implements
	FragmentInspectionSetupClientSelection.OnBuyerAgentSelectedListener,
	FragmentInspectionSetupClientSelection.OnSellerAgentSelectedListener,
	FragmentInspectionSetupReportNumber.OnReportNumberChangedListener,
	FragmentInspectionSetupClientInfo.OnClientInfoChangedListener,
	FragmentInspectionSetupPropertyLocation.OnPropertyLocationChangedListener,
	FragmentInspectionSetupPropertyInfo.OnPropertyInfoChangedListener,
	FragmentInspectionSetupScheduler.OnDateInspectionScheduledChangedListener,
	FragmentInspectionSetupBilling.OnBillingDetailsChangedListener {

	//database
	public ServiceApplicationDatabase myDB;
	private boolean mBound;

	//fragment variables
	private InspectionSetupFragment mCurrentFragment;
	public enum InspectionSetupFragment{
		ReportNumber,
		ClientInfo,
		AgentSelection,
		PropertyLocation,
		PropertyInfo,
		Scheduler,
		Billing
	}

	//Navigation Drawer
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private String[] mInspectionSetupItems;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;

	//Menu Items
	private MenuItem mSave;
	static final private int MENU_SAVE = Menu.FIRST;
	static final private int MENU_CANCEL = Menu.FIRST + 1;

	//Action Bar title
	private CharSequence mTitle;

	//Inspection Info
	private CInspectionSetup mInspectionSetup;
	private Boolean mNeedSave_InspectionSetup = false;
	private String mCurrentState = "";

	//client variables
	private CClient mCurrentClient = null;
	private CClient mChangedClient = null;
	private Boolean mIsClientNew = true;
	private Boolean mDeleteCurrentClient = false;
	private Boolean mNeedSave_ClientInfo = false;

	//agents variables
	private CAgent mBuyersAgent = new CAgent();
	private int mSelectedBuyersAgentIndex;
	private Boolean mRemoveBuyersAgent = false;
	private Boolean mNeedSave_BuyersAgent = false;

	private CAgent mSellersAgent = new CAgent();
	private int mSelectedSellersAgentIndex;
	private Boolean mRemoveSellersAgent = false;
	private Boolean mNeedSave_SellersAgent = false;

	//property location variables
	private CPropertyLocation mPropertyLocation = new CPropertyLocation();
	private Boolean mNeedSave_PropertyLocation = false;

	//scheduler variables
	private Boolean mNeedSave_SchedulerInformation = false;

	//billing variables
	private Boolean mNeedSave_BillingInformation = false;

	//STRINGS FOR PREFERENCES
	public static final String CURRENT_LOCATION = "CURRENT_LOCATION";
	public static final String CURRENT_STATE = "CURRENT_STATE";

	//STRINGS  FOR BUNDLE EXCHANGES
	public static final String INSPECTION_SETUP_REPORT_NUMBER = "INSPECTION_SETUP_REPORT_NUMBER";
	public static final String INSPECTION_SETUP_IS_NEW = "INSPECTION_SETUP_IS_NEW";

	//strings to tag and id fragments
	private static final String REPORT_NUMBER = "REPORT_NUMBER";
	private static final String CLIENT_INFO = "CLIENT_INFO";
	private static final String AGENT_SELECTION = "AGENT_SELECTION";
	private static final String PROPERTY_LOCATION = "PROPERTY_LOCATION";
	private static final String PROPERTY_INFO = "PROPERTY_INFO";
	private static final String SCHEDULER = "SCHEDULER";
	private static final String BILLING = "BILLING";

	//*************************************************************************************************
	//Life Cycle

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//set layout
		setContentView(R.layout.activity_inspection_setup);

		//setup action bar
		setupActionBar();

		//set up navigation drawer
		setupNavigationDrawer();

		//create a new inspection
		mInspectionSetup = new CInspectionSetup(this);

		//check if there is a selected inspection setup that should be loaded
		Bundle extras = this.getIntent().getExtras();

		//if there is any, get the report number from the intent and store it
		//it will be loaded once this activity binds to the database
		if (extras != null){
			//there was an inspection setup selected
			//save the report number so it can be loaded later
			mInspectionSetup.setReportNumber(extras.getInt(ActivityInspectionSetupSelection.INSPECTION_SETUP_SELECTION_REPORT_NUMBER));
			//set the inspection to not new
			mInspectionSetup.setIsNew(false);

		}else{
			//there was no report selected so set this up for a new inspection setup

			//set the report number to an initial value that signals this is a new inspection
			//once the database is bound we will retrieve a valid report number
			mInspectionSetup.setReportNumber(-1);

			//retrieve shared preferences
			SharedPreferences sharedPrefs = getSharedPreferences(CURRENT_LOCATION, Activity.MODE_PRIVATE);
			mCurrentState = sharedPrefs.getString(CURRENT_STATE, "CA");
		}

		//connect to the database
		bindToDatabaseService();

		mBuyersAgent = new CAgent();
		mSellersAgent = new CAgent();

	}

	@Override
	public void onStart(){super.onStart();}

	@Override
	public void onPause(){
		super.onPause();
	}

	@Override
	public void onStop(){
		super.onStop();

		//unbind from the database
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}

	}

	private void notifyFragments() {
		FragmentManager fm = getFragmentManager();

		//report number
		FragmentInspectionSetupReportNumber reportNumberFragment =
			(FragmentInspectionSetupReportNumber) fm.findFragmentByTag(REPORT_NUMBER);

		if(reportNumberFragment!=null){
			reportNumberFragment.notifySaved();
		}

		//client info
		FragmentInspectionSetupClientInfo clientInfoFragment =
			(FragmentInspectionSetupClientInfo) fm.findFragmentByTag(CLIENT_INFO);

		if(clientInfoFragment!=null){
			clientInfoFragment.notifySaved();
		}

		//property location
		FragmentInspectionSetupPropertyLocation propertyLocationFragment =
			(FragmentInspectionSetupPropertyLocation) fm.findFragmentByTag(PROPERTY_LOCATION);

		if(propertyLocationFragment!=null){
			propertyLocationFragment.notifySaved();
		}

		//property information
		FragmentInspectionSetupPropertyInfo propertyInfoFragment =
			(FragmentInspectionSetupPropertyInfo) fm.findFragmentByTag(PROPERTY_INFO);

		if(propertyInfoFragment!=null){
			propertyInfoFragment.notifySaved();
		}

		//schedule inspection
		FragmentInspectionSetupScheduler schedulerFragment =
			(FragmentInspectionSetupScheduler) fm.findFragmentByTag(SCHEDULER);

		if(schedulerFragment!=null){
			schedulerFragment.notifySaved();
		}

		//billing
		FragmentInspectionSetupBilling billingFragment =
			(FragmentInspectionSetupBilling) fm.findFragmentByTag(BILLING);

		if(billingFragment!=null){
			billingFragment.notifySaved();
		}


	}


	//*****************************************************************************************
	//Action Bar setup
	private void setupActionBar(){
		// Find the toolbar view and set as ActionBar
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
	}

	//****************************************************************************************************
	//Menu

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		Menu mMenu = menu;
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_inspection_setup, menu);

		int groupID = 0;
		int menuItemID = MENU_SAVE;
		int menuItemOrder = Menu.FIRST;
		int menuItemText = R.string.action_bar_save;

		mSave = mMenu.add(groupID, menuItemID, menuItemOrder, menuItemText);
		mSave.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mSave.setIcon(android.R.drawable.ic_menu_add);
		mSave.setEnabled(false); //on first load should be disabled.

		groupID = 0;
		menuItemID = MENU_CANCEL;
		menuItemOrder = Menu.FIRST + 1;
		menuItemText = R.string.action_bar_cancel;

		MenuItem mCancel = mMenu.add(groupID, menuItemID, menuItemOrder, menuItemText);
		mCancel.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		mCancel.setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//save changes and unload or discard changes and unload
		switch (item.getItemId()) {

			case android.R.id.home:
				//check to see if drawer is open
				if(mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
					// navigate up from this
					NavUtils.navigateUpFromSameTask(this);
					mDrawerLayout.closeDrawer(Gravity.LEFT); //CLOSE Nav Drawer!
				}else{
					// open drawer
					mDrawerLayout.openDrawer(Gravity.LEFT); //OPEN Nav Drawer!
				}
				return true;

			case MENU_SAVE:
				//save changes
				saveAllChanges();
				notifyFragments();
				return true;

			case MENU_CANCEL:
				//discard all changes
				this.finish();
				return true;

		}
		return super.onOptionsItemSelected(item);
	}


	//*************************************************************************************************
	//Navigation Drawer

	private void setupNavigationDrawer() {
		mTitle = mDrawerTitle = getTitle();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mInspectionSetupItems = getResources().getStringArray(R.array.SetupInspectionTaskList);

		mDrawerList.setAdapter((new ArrayAdapter<>(this,
			R.layout.listview_task_list,
			mInspectionSetupItems)));
		mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		mDrawerList.setOnItemClickListener(new CInspectionSetupDrawerItemClickListener());

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
			R.string.drawer_open, R.string.drawer_close) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				getSupportActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				getSupportActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.addDrawerListener(mDrawerToggle);

		mDrawerToggle.setDrawerIndicatorEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		//add initial frame
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.add(R.id.content_frame, new FragmentInspectionSetupIntroMessage());
		ft.commit();
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content view
		//boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		//menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	//*************************************************************************************************
	//Listeners

	@Override
	public void onBuyerAgentSelected(int agentIndex) {

		//check to see if "none" is selected
		if(agentIndex == -1){
			//"none" is selected

			//set flag to remove any buyers agent if one exists
			mRemoveBuyersAgent = mSelectedBuyersAgentIndex != -1;

		}else{
			//check to see if this is the same agent or a different one
			if(mSelectedBuyersAgentIndex!= agentIndex){
				//a different agent was selected

				//copy the index value
				mSelectedBuyersAgentIndex = agentIndex;

				//set save needed flag
				mNeedSave_BuyersAgent = true;
			}else{
				mNeedSave_BuyersAgent = false;
			}
		}

		if (mRemoveBuyersAgent || mNeedSave_BuyersAgent){
			//enable saving
			notifyOfNewData();
		}
	}

	..............................................................................................................

	@Override
	public void onReportNumberChanged(int reportNumber) {
		//save data locally
		mInspectionSetup.setReportNumber(reportNumber);

		//set flag that this setup needs to be saved
		mNeedSave_InspectionSetup = true;

		//set flag that there is data to save
		notifyOfNewData();
	}

	@Override
	public void onClientInfoChanged(Bundle extras) {

		if(extras.getBoolean(FragmentInspectionSetupClientInfo.CLIENT_HAS_CLIENT_CHANGED)){
			//set flag that this info needs to be saved.
			mNeedSave_ClientInfo = true;

			//copy incoming data about client
			mChangedClient.setName(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_NAME));
			mChangedClient.setAddress(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_ADDRESS));
			mChangedClient.setCity(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_CITY));
			mChangedClient.setState(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_STATE));
			mChangedClient.setZip(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_ZIP));
			mChangedClient.setEmail(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_EMAIL));
			mChangedClient.setNumber(extras.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_NAME));

			//check to see if this is a new client or an update to existing client
			if(extras.getBoolean(FragmentInspectionSetupClientInfo.CLIENT_IS_NEW)){
				//this is a new client
				mIsClientNew = true;

				//we have to delete the previous client from the database
				mDeleteCurrentClient = true;

			}else{

				//client is not new
				mIsClientNew = false;

				//we do not have to delete the previous client from the database
				mDeleteCurrentClient = false;
			}

			//enable saving
			notifyOfNewData();

		}else{
			//set flag that there is no info to save.
			mNeedSave_ClientInfo = false;
		}

	}
	
	..............................................................................................................


	//*******************************************************************************************
	//Database

	//Creating a Service Connection for Service binding
	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// Called when the connection is made.
			myDB = ((ServiceApplicationDatabase.MyBinder)service).getService();
			mBound = true;

			prepareInspectionSetup();
		}

		public void onServiceDisconnected(ComponentName className) {
			// Received when the service unexpectedly disconnects.
			myDB = null;
			mBound = false;
		}
	};

	private void bindToDatabaseService() {
		//Bind to the service
		Intent bindIntent = new Intent(this, ServiceApplicationDatabase.class);
		bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE);
	}

	//**********************************************************************************************
	//PREPARE INSPECTION SETUP
	private void prepareInspectionSetup(){
		//check to see if this is a new inspection setup
		if(mInspectionSetup.isUnmodified()){
			//this is a new inspection setup
			//retrieve the next available inspection number and assign it to this instance
			mInspectionSetup.setReportNumber(myDB.nextAvailableReportNumber());

			//initialize agent indexes
			mSelectedBuyersAgentIndex = -1;
			mSelectedSellersAgentIndex = -1;

		}else{
			//this is not a new inspection setup

			//retrieve the inspection setup from the database
			mInspectionSetup = myDB.getInspection(mInspectionSetup.getReportNumber()).toInspectionSetup();

			//retrieve the client from the database
			mCurrentClient = myDB.getInspectionClient(mInspectionSetup.getReportNumber());

			//retrieve the property location
			mPropertyLocation = myDB.getPropertyLocation(mInspectionSetup.getPropertyLocationIndex());

			//retrieve the buyer's agent for this inspection setup
			mBuyersAgent = myDB.getAgentInfo(myDB.getInspectionBuyersAgentIndex(mInspectionSetup.getReportNumber()));

			//copy agent index to the local editable
			mSelectedBuyersAgentIndex = mBuyersAgent.getDatabaseIndex();

			//retrieve the seller's agent for this inspection setup
			mSellersAgent = myDB.getAgentInfo(myDB.getInspectionSellersAgentIndex(mInspectionSetup.getReportNumber()));

			//copy agent index to the local editable
			mSelectedSellersAgentIndex = mSellersAgent.getDatabaseIndex();

		}
	}

	..............................................................................................................


	//**********************************************************************************************
	//INSPECTION I/O

	private void promptToSaveChanges(){
		//check to see if there is any info that has changed that needs to be saved
		if(mNeedSave_InspectionSetup||mNeedSave_ClientInfo||mNeedSave_BuyersAgent||mNeedSave_SellersAgent||
			mNeedSave_PropertyLocation||mNeedSave_SchedulerInformation||mNeedSave_BillingInformation){
			//prompt user with a dialog to see if they want to save any changes

			AlertDialog.Builder saveAlertDialog = new AlertDialog.Builder(this);

			String title = "Save Changes?";
			String message = "There are changes that have not been saved.";

			String button1 = "Yes";
			String button2 = "No";

			saveAlertDialog.setTitle(title);
			saveAlertDialog.setMessage(message);

			saveAlertDialog.setPositiveButton(button1, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//call save changes
					saveAllChanges();

					//notify fragments that info is saved
					notifyFragments();
				}
			});

			saveAlertDialog.setNegativeButton(button2, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//do nothing since user does not want to save
					//all changes will not be saved to the database
				}
			});

			saveAlertDialog.create().show();
		}
	}

	private void saveAllChanges(){
		// first get the data in the open fragment

		//open the fragment manager
		FragmentManager fm = getFragmentManager();

		//retrieve the data from the active fragment based on type
		switch(mCurrentFragment){
			case ReportNumber:{
				//this data is always up to date since it
				//is validated and sent as soon as it is entered
				//no retrieval necessary here
				break;
			}

			case ClientInfo:{

				//get the fragment
				FragmentInspectionSetupClientInfo frag =
					(FragmentInspectionSetupClientInfo) fm.findFragmentByTag(CLIENT_INFO);

				//get data
				Bundle data = frag.getData();

				//check if data has changed
				if(data.getBoolean(FragmentInspectionSetupClientInfo.CLIENT_HAS_CLIENT_CHANGED)){
					//make sure changed client is valid
					if(mChangedClient == null){
						mChangedClient = new CClient();
					}

					//copy the data
					mChangedClient.setName(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_NAME));
					mChangedClient.setAddress(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_ADDRESS));
					mChangedClient.setCity(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_CITY));
					mChangedClient.setState(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_STATE));
					mChangedClient.setZip(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_ZIP));
					mChangedClient.setNumber(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_NUMBER));
					mChangedClient.setEmail(data.getString(FragmentInspectionSetupClientInfo.CLIENT_INFO_EMAIL));

					//set the flags
					mNeedSave_ClientInfo = data.getBoolean(FragmentInspectionSetupClientInfo.CLIENT_HAS_CLIENT_CHANGED);
					mIsClientNew = data.getBoolean(FragmentInspectionSetupClientInfo.CLIENT_IS_NEW);
				}

				break;
			}

			case AgentSelection:{
				//this data is always up to date since it
				//is validated and sent as soon as it is entered
				//no retrieval necessary here
				break;
			}

			case PropertyLocation:{

				//get the fragment
				FragmentInspectionSetupPropertyLocation frag =
					(FragmentInspectionSetupPropertyLocation) fm.findFragmentByTag(PROPERTY_LOCATION);

				//get data
				Bundle data = frag.getData();

				//copy the data
				mPropertyLocation.setPropertyAddress(data.getString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_ADDRESS));
				mPropertyLocation.setPropertyCity(data.getString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_CITY));
				mPropertyLocation.setPropertyState(data.getString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_STATE));
				mPropertyLocation.setPropertyZip(data.getString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_ZIP));

				//set the flags
				mNeedSave_PropertyLocation = data.getBoolean(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_HAS_CHANGED);
				break;
			}

			case PropertyInfo:{

				//get the fragment
				FragmentInspectionSetupPropertyInfo frag =
					(FragmentInspectionSetupPropertyInfo) fm.findFragmentByTag(PROPERTY_INFO);

				//get data
				Bundle data = frag.getData();

				//copy the data
				mInspectionSetup.setBedroomCount((short) data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_BEDROOM_COUNT));
				mInspectionSetup.setBathroomCount((short) data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_BATHROOM_COUNT));
				mInspectionSetup.setFloorCount((short) data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_FLOOR_COUNT));
				mInspectionSetup.setSquareFootage(data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_SQUARE_FOOTAGE));
				mInspectionSetup.setUtilitiesOn(data.getBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_UTILITIES_STATUS));
				mInspectionSetup.setHasBasement(data.getBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_BASEMENT));
				mInspectionSetup.setHasCrawlspace(data.getBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_CRAWLSPACE));
				mInspectionSetup.setHasGarage(data.getBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_GARAGE));
				mInspectionSetup.setIsGarageAttached(data.getBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_GARAGE_ATTACHED));
				mInspectionSetup.setYearStructureBuilt(data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_YEAR_BUILT));
				mInspectionSetup.setStructureAgeYears((short) data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_YEARS_OLD));
				mInspectionSetup.setRoofAgeYears((short) data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_ROOF_AGE_YEARS));
				mInspectionSetup.setBuildingTypeIndex(data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_BUILDING_TYPE_INDEX));
				mInspectionSetup.setStateOfOccupancyIndex(data.getInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_STATE_OF_OCCUPANCY_INDEX));

				//set the flags
				mNeedSave_InspectionSetup = data.getBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_CHANGED);

				break;
			}

			case Scheduler:{

				//get the fragment
				FragmentInspectionSetupScheduler frag =
					(FragmentInspectionSetupScheduler) fm.findFragmentByTag(SCHEDULER);

				//get data
				Bundle data = frag.getData();

				//copy the data
				mInspectionSetup.setDateInspectionScheduled(data.getLong(FragmentInspectionSetupScheduler.SCHEDULER_APPOINTMENT));

				//set the flags
				mNeedSave_SchedulerInformation = data.getBoolean(FragmentInspectionSetupScheduler.SCHEDULER_DATA_CHANGED);

				break;
			}

			case Billing:{

				//get the fragment
				FragmentInspectionSetupBilling frag =
					(FragmentInspectionSetupBilling) fm.findFragmentByTag(BILLING);

				//get data
				Bundle data = frag.getData();

				//copy the data
				//TODO

				//set the flags
				//TODO

				break;
			}

			default:{

			}
		}

		//********************************************************
		//INSPECTION SETUP

		//is there inspection setup data to save
		if(mNeedSave_InspectionSetup){
			//convert setup to an inspection
			CInspection inspection = mInspectionSetup.toInspection();

			//is this a new inspection
			if (inspection.isUnmodified()){

				//add inspection to the database
				if(myDB.addNewInspection(inspection) != -1){
					//set flag that this setup is no longer new
					mInspectionSetup.setIsNew(false);
				}

			}else{
				//update existing inspection
				myDB.updateInspection(inspection);
			}

			//reset save flag
			mNeedSave_InspectionSetup = false;
		}

		//*******************************************************
		//CLIENT
		//is there client info to save or update
		if(mNeedSave_ClientInfo){
			//is the client new
			if(mIsClientNew){
				//client is new

				//add client to database and
				//relate client to inspection
				myDB.addInspectionClient(mInspectionSetup.getReportNumber(),
					(int) myDB.addNewClient(mChangedClient));

				//should we delete the current client
				if(mDeleteCurrentClient){
					//delete the current client from the database
					myDB.removeClientInfo(mCurrentClient.getDatabaseIndex());

					//remove the inspection association from the databse
					myDB.removeInspectionClient(mInspectionSetup.getReportNumber(), mCurrentClient.getDatabaseIndex());
				}



			}else{
				//client is not new

				//update client in database
				myDB.updateClientInfo(mChangedClient);
			}

			//assign changed client to current client
			try {
				mCurrentClient = mChangedClient.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}

		}

		//********************************************************
		//BUYERS AGENT
		//check to see if we have to delete the previous agent association
		if(mRemoveBuyersAgent || mNeedSave_BuyersAgent){
			//delete the existing agent association from the database
			myDB.removeInspectionAgent(mInspectionSetup.getReportNumber(), mBuyersAgent.getDatabaseIndex());
		}

		//check to see if we need to save buyers agent info
		if(mNeedSave_BuyersAgent){
			//retrieve agent from database and
			//assign new agent to local copy
			mBuyersAgent = myDB.getAgentInfo(mSelectedBuyersAgentIndex);

			//create association in database
			myDB.addInspectionAgent(mInspectionSetup.getReportNumber(),
				CAgent.BUYERS_AGENT, mBuyersAgent.getDatabaseIndex());
		}

		..............................................................................................................

		//*********************************************************
		//PROPERTY LOCATION
		//is there data to save
		if(mNeedSave_PropertyLocation){
			//check to see if the location is new or needs to be updated
			if(mPropertyLocation.getDatabaseIndex() == -1){
				//location is new
				//add location to database and save index to inspection setup
				mInspectionSetup.setPropertyLocationIndex((int)myDB.addPropertyLocation(mPropertyLocation));

				//set inspection setup save flag
				mNeedSave_InspectionSetup = true;
			}else{
				//location is not new
				//update location in database
				myDB.updatePropertyLocation(mPropertyLocation);
			}

			//reset save flag
			mNeedSave_PropertyLocation = false;

		}

		..............................................................................................................

		//*********************************************************
		//BUTTON STATE
		//disable the save button since all data was just saved
		mSave.setEnabled(false);
	}

	private class CInspectionSetupDrawerItemClickListener implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
		FragmentManager fm = getFragmentManager();
		Fragment oldFragment = fm.findFragmentById(R.id.content_frame);
		if (oldFragment == null){

		}
		FragmentTransaction ft = fm.beginTransaction();

		Fragment doesExist;

		//load the different fragments based on what is selected
		switch(position) {
			case 0:	//Report Number
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), REPORT_NUMBER)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(REPORT_NUMBER);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{

					//open bundle
					Bundle extras = new Bundle();

					//pack report number
					extras.putInt("INSPECTION_SETUP_REPORT_NUMBER", mInspectionSetup.getReportNumber());

					//pack new or saved based on status of inspection setup
					if(mInspectionSetup.isUnmodified()){
						extras.putBoolean("INSPECTION_SETUP_IS_NEW", true);
					}else{
						extras.putBoolean("INSPECTION_SETUP_IS_NEW", false);
					}

					//create fragment
					FragmentInspectionSetupReportNumber newFrag =
						new FragmentInspectionSetupReportNumber();

					//add extras
					newFrag.setArguments(extras);

					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());

					//make a new copy of this fragment and add it to the view group
					ft.replace(R.id.content_frame, newFrag, REPORT_NUMBER);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.ReportNumber;

				break;
			}
			case 1:	//Client Information
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), CLIENT_INFO)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(CLIENT_INFO);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{

					//bundle for extras
					Bundle extras = new Bundle();

					//pack new or saved based on status of inspection setup
					if(mInspectionSetup.isUnmodified()){
						//do not pack any data
						extras = null;
					}else{
						//pack current Client data for fragment
						extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_NAME,
							mCurrentClient.getName());
						extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_ADDRESS,
							mCurrentClient.getAddress());
						extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_CITY,
							mCurrentClient.getCity());
						extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_ZIP,
							mCurrentClient.getZip());
						extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_NUMBER,
							mCurrentClient.getNumber());
						extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_EMAIL,
							mCurrentClient.getEmail());

						if (mCurrentClient.getDatabaseIndex()==-1){
							extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_STATE,
								mCurrentState);
						}else{
							extras.putString(FragmentInspectionSetupClientInfo.CLIENT_INFO_STATE,
								mCurrentClient.getState());
						}
					}

					//create fragment and add extras
					FragmentInspectionSetupClientInfo newFrag =
						new FragmentInspectionSetupClientInfo();
					newFrag.setArguments(extras);
					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());
					//make a new copy of this fragment and add it to the view group
					ft.replace(R.id.content_frame, newFrag, CLIENT_INFO);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.ClientInfo;

				break;
			}
			case 2:	//CAgent Selection
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), AGENT_SELECTION)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(AGENT_SELECTION);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{
					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());
					//make a new copy of this fragment and add it to the view group
					FragmentInspectionSetupClientSelection newFragment = new FragmentInspectionSetupClientSelection();

					//check to see if the agents have been selected
					if (mBuyersAgent.getDatabaseIndex() != -1 || mSellersAgent.getDatabaseIndex() != -1){
						//one or the other is saved so package the data

						//bundle for data
						Bundle extras = new Bundle();

						//is there a buyer's agent
						if(mBuyersAgent.getDatabaseIndex() != -1 ){
							extras.putString(FragmentInspectionSetupClientSelection.AGENT_SELECTION_BUYERS_AGENT_NAME, mBuyersAgent.getAgentName());
						}

						//is there a sellers's agent
						if(mSellersAgent.getDatabaseIndex() != -1 ){
							extras.putString(FragmentInspectionSetupClientSelection.AGENT_SELECTION_SELLERS_AGENT_NAME, mSellersAgent.getAgentName());
						}

						//send bundle to fragment
						newFragment.setArguments(extras);

					}

					ft.replace(R.id.content_frame, newFragment, AGENT_SELECTION);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.AgentSelection;

				break;
			}
			case 3:	//Property Location
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), PROPERTY_LOCATION)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(PROPERTY_LOCATION);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{
					//pack current Property Location data for fragment
					Bundle extras = new Bundle();

					extras.putString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_ADDRESS,
						mPropertyLocation.getPropertyAddress());
					extras.putString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_CITY,
						mPropertyLocation.getPropertyCity());
					extras.putString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_ZIP,
						mPropertyLocation.getPropertyZip());

					if (mPropertyLocation.getDatabaseIndex()==-1){
						extras.putString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_STATE,
							mCurrentState);
					}else{
						extras.putString(FragmentInspectionSetupPropertyLocation.PROPERTY_LOCATION_STATE,
							mPropertyLocation.getPropertyState());
					}
					//create fragment and add extras
					FragmentInspectionSetupPropertyLocation newFrag =
						new FragmentInspectionSetupPropertyLocation();
					newFrag.setArguments(extras);
					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());
					//make a new copy of this fragment and add it to the view group
					ft.replace(R.id.content_frame, newFrag, PROPERTY_LOCATION);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.PropertyLocation;

				break;
			}
			case 4:	//Property Information
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), PROPERTY_INFO)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(PROPERTY_INFO);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{

					//pack current Property Info data for fragment
					Bundle extras = new Bundle();

					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_BEDROOM_COUNT,
						mInspectionSetup.getBedroomCount());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_BATHROOM_COUNT,
						mInspectionSetup.getBathroomCount());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_FLOOR_COUNT,
						mInspectionSetup.getFloorCount());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_SQUARE_FOOTAGE,
						mInspectionSetup.getSquareFootage());

					extras.putBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_UTILITIES_STATUS,
						mInspectionSetup.isUtilitiesOn());
					extras.putBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_BASEMENT,
						mInspectionSetup.hasBasement());
					extras.putBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_CRAWLSPACE,
						mInspectionSetup.hasCrawlspace());
					extras.putBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_HAS_GARAGE,
						mInspectionSetup.hasGarage());
					extras.putBoolean(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_GARAGE_ATTACHED,
						mInspectionSetup.isGarageAttached());

					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_YEAR_BUILT,
						mInspectionSetup.getYearStructureBuilt());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_YEARS_OLD,
						mInspectionSetup.getStructureAgeYears());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_ROOF_AGE_YEARS,
						mInspectionSetup.getRoofAgeYears());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_BUILDING_TYPE_INDEX,
						mInspectionSetup.getBuildingTypeIndex());
					extras.putInt(FragmentInspectionSetupPropertyInfo.PROPERTY_INFO_STATE_OF_OCCUPANCY_INDEX,
						mInspectionSetup.getStateOfOccupancyIndex());

					//create fragment
					FragmentInspectionSetupPropertyInfo newFrag =
						new FragmentInspectionSetupPropertyInfo();

					//attach extras to fragment
					newFrag.setArguments(extras);


					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());
					//make a new copy of this fragment and add it to the view group
					ft.replace(R.id.content_frame, newFrag, PROPERTY_INFO);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.PropertyInfo;

				break;
			}
			case 5:	//Schedule Inspection
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), SCHEDULER)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(SCHEDULER);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{
					//pack current Schedule data for fragment
					Bundle extras = new Bundle();

					//copy the date to the bundle
					extras.putLong(FragmentInspectionSetupScheduler.SCHEDULER_APPOINTMENT, mInspectionSetup.getDateInspectionScheduled());

					//create fragment
					FragmentInspectionSetupScheduler newFrag =
						new FragmentInspectionSetupScheduler();

					//attach extras to fragment
					newFrag.setArguments(extras);

					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());
					//make a new copy of this fragment and add it to the view group
					ft.replace(R.id.content_frame, newFrag,SCHEDULER);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.Scheduler;

				break;
			}
			case 6:	//Billing
			{
				//check to see if this fragment is already the current one being displayed
				if (Objects.equals(oldFragment.getTag(), BILLING)){
					//do nothing, exit out
					break;
				}

				//check if this fragment is already on the stack
				doesExist = fm.findFragmentByTag(BILLING);

				if (doesExist != null){
					//fragment is on the stack and needs to be brought forward.
					ft.replace(R.id.content_frame, doesExist);
					ft.commit();
				}else{
					//log fragment transaction to the back stack
					ft.addToBackStack(oldFragment.toString());
					//make a new copy of this fragment and add it to the view group
					ft.replace(R.id.content_frame, new FragmentInspectionSetupBilling(),BILLING);
					ft.commit();
				}

				//set active fragment tag
				mCurrentFragment = InspectionSetupFragment.Billing;

				break;
			}
		}

		// Highlight the selected item, update the title, and close the drawer
		mDrawerList.setItemChecked(position, true);
		setTitle(mInspectionSetupItems[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

}
