package com.xxxxxxxxx;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentInspectionSetupPropertyInfo extends Fragment {
	
	//loading progress dialog
	private ProgressDialog loadingDialog;
	
	//range of years to determine list of years in spinners
	private int mStructureAgeRange;
	
	//current focused view
	private int mFocusedView;
	
	//flag to indicate user has changed info and it is not saved
	private Boolean mInfoHasChanged = false;
	
	//property info
	..............................................................................................................
	private int mYearBuilt;
	..............................................................................................................
	
	//view handles
	private View root;
	
	..............................................................................................................
	private EditText yearBuilt;
	..............................................................................................................
	
	//ID Strings for bundle exchange to and from parent activity
		..............................................................................................................
	public static final String PROPERTY_INFO_YEAR_BUILT = "com.xxxxxxxxx.PROPERTY_INFO_YEAR_BUILT";
		..............................................................................................................
	
	//handle to parent
	private ActivityInspectionSetup mParent;
	
	//listener from parent activity
	private OnPropertyInfoChangedListener mListener;
	
	//interface required from parent activity
	public interface OnPropertyInfoChangedListener{
		void onPropertyInfoChanged(Bundle extras);
	}
	
	//STRINGS FOR PREFERENCES
	private static final String STRUCTURE_AGE_RANGE = "STRUCTURE_AGE_RANGE";
	
	//*******************************************************************************************
	
	//this method packages the data in this fragment
	//and returns it to the caller
	public Bundle getData(){
		Bundle data = new Bundle();
		
		if(mInfoHasChanged){
			//set flag that data changed
			data.putBoolean(PROPERTY_INFO_HAS_CHANGED, mInfoHasChanged);
			
			//bundle the rest of the data
			..............................................................................................................
			data.putInt(PROPERTY_INFO_YEAR_BUILT, mYearBuilt);
			..............................................................................................................
			
		}else{
			//set flag that data has not changed
			data.putBoolean(PROPERTY_INFO_HAS_CHANGED, mInfoHasChanged);
		}

		return data;
	}
	
	@Override
    public void onAttach(Context context) {
        super.onAttach(context);

		Activity a = new Activity();
		if (context instanceof Activity){
			a = (Activity) context;
		}
        
        //bind the listener from the calling activity
        try {
            mListener = (OnPropertyInfoChangedListener) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString() + " must implement onPropertyInfoChanged Listener");
        }
        
      //retrieve shared preferences
      SharedPreferences sharedPrefs = a.getSharedPreferences(STRUCTURE_AGE_RANGE, Activity.MODE_PRIVATE);
      mStructureAgeRange = sharedPrefs.getInt(STRUCTURE_AGE_RANGE, 100);
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		//indicate that the system should save any info if fragment is lost
		setRetainInstance(true);
		
		//get handle for parent
		mParent = (ActivityInspectionSetup)this.getActivity();
		
		//check to see if there is a saved state
		if (savedInstanceState == null){
			//pull intent data if present and copy it
			Bundle incomingData = getArguments();
			if(incomingData != null){
				..............................................................................................................
				mYearBuilt = incomingData.getInt(PROPERTY_INFO_YEAR_BUILT);
				..............................................................................................................
				
			}
		}else{
			//there is a saved state so restore data from there
			..............................................................................................................
			mYearBuilt = savedInstanceState.getInt(PROPERTY_INFO_YEAR_BUILT);
			..............................................................................................................
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		
		//inflate the layout from resource
		return inflater.inflate(R.layout.fragment_inspection_setup_property_info,
								container,
								false);
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		
		//associate the individual listeners to their respective views
		
		//get the root view
		root = this.getView();
		
		//display loading dialog
		loadingDialog = new ProgressDialog(root.getContext());
		loadingDialog.setTitle("Loading...");
		loadingDialog.setMessage("Please wait.");
		loadingDialog.setCancelable(false);
		loadingDialog.setIndeterminate(true);
		loadingDialog.show();
		
		//get handles for controls for use throughout other listeners and methods
		..............................................................................................................
		yearBuilt = (EditText) root.findViewById(R.id.edittext_year_built);
		..............................................................................................................
		
		//ON EDITOR ACTION LISTENER
		yearBuilt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
	        
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		           
		           switch(actionId){
		           
			           case EditorInfo.IME_ACTION_NEXT:{
			        	   //make sure the field isn't blank or missing numbers.  if it is change it to a default value
			        	   
							//check to see if it is empty first
							if(yearBuilt.getText().toString().contentEquals("")){
								//edit text is blank
								//notify user this can't be blank
								Toast.makeText(yearBuilt.getContext(), "This value cannot be blank.  Value set to current year.", Toast.LENGTH_LONG).show();
								
								//get present year
								Calendar c = Calendar.getInstance(); 
								int presentYear=c.get(Calendar.YEAR);
								
								//set it to the present year
								yearBuilt.setText(String.valueOf(presentYear));
				        		
							}else 
								//check to see that there are enough characters for a year
							
								if (yearBuilt.length() < 4){
									//this is not enough chars
									//display a toast to notify user
									Toast.makeText(yearBuilt.getContext(), "Not enough numbers to make a valid year. Value set to current year.", Toast.LENGTH_LONG).show();
									
									//get present year
									Calendar c = Calendar.getInstance(); 
									int presentYear=c.get(Calendar.YEAR);
									
									//set it to the present year
									yearBuilt.setText(String.valueOf(presentYear));
							}
							
							//validate the year
			        		validateStructureAge();
							
							//check to see if data is different
							int testInt = Integer.parseInt(yearBuilt.getText().toString());

							if(mYearBuilt != testInt){
								//data is not the same
								
								//set flag that data has changed
								mInfoHasChanged = true;

								//copy data to local variable
								mYearBuilt = testInt;

								//notify parent that there is data to save
								mParent.notifyOfNewData();
							}else{
								//set flag that data has not changed
								mInfoHasChanged = false;
							}
							
							return false;
			           }
		           
			           case EditorInfo.IME_ACTION_DONE:{
			        	   	//make sure the field isn't blank or missing numbers.  if it is change it to a default value
			        	   
							//check to see if it is empty first
							if(yearBuilt.getText().toString().contentEquals("") ){
								//edit text is blank
								//notify user this can't be blank
								Toast.makeText(yearBuilt.getContext(), "This value cannot be blank.  Value set to current year.", Toast.LENGTH_LONG).show();
								
								//get present year
								Calendar c = Calendar.getInstance(); 
								int presentYear=c.get(Calendar.YEAR);
								
								//set it to the present year
								yearBuilt.setText(String.valueOf(presentYear));
							}else 
								//check to see that there are enough characters for a year
							
								if (yearBuilt.length() < 4){
								//this is not enough chars
								//display a toast to notify user
								Toast.makeText(yearBuilt.getContext(), "Not enough numbers to make a valid year. Value set to current year.", Toast.LENGTH_LONG).show();
								
								//get present year
								Calendar c = Calendar.getInstance(); 
								int presentYear=c.get(Calendar.YEAR);
								
								//set it to the present year
								yearBuilt.setText(String.valueOf(presentYear));
							}
							
							//validate the year
			        		validateStructureAge();
							
							//check to see if data is different
							int testInt = Integer.parseInt(yearBuilt.getText().toString());

							if(mYearBuilt != testInt){
								//data is not the same
								
								//set flag that data has changed
								mInfoHasChanged = true;

								//copy data to local variable
								mYearBuilt = testInt;

								//notify parent that there is data to save
								mParent.notifyOfNewData();
							}else{
								//set flag that data has not changed
								mInfoHasChanged = false;
							}
							
							//hide the soft keyboard
							InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
							mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
							
							return true;
			           }
			           
			           default:{
			        	   return false;
			           }
		           }
			}
	    });
		
		//ON FOCUS CHANGED LISTENER
		yearBuilt.setOnFocusChangeListener(new View.OnFocusChangeListener(){

					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						//make sure the field is not blank and that it has enough numbers for a valid year
						//if it doesn't then notify the user and change it to a default value
						
						// check if view lost focus
						if(!hasFocus){
							//view has lost focus so validate the input
							//make sure the field isn't blank or missing numbers.  if it is change it to a default value

							//check to see if it is empty first
							if(yearBuilt.getText().toString().contentEquals("")){
								//edit text is blank
								//notify user this can't be blank
								Toast.makeText(yearBuilt.getContext(), "This value cannot be blank.  Value set to current year.", Toast.LENGTH_LONG).show();

								//get present year
								Calendar c = Calendar.getInstance(); 
								int presentYear=c.get(Calendar.YEAR);

								//set it to the present year
								yearBuilt.setText(String.valueOf(presentYear));

							}else 
							//check to see that there are enough characters for a year

							if (yearBuilt.length() < 4){
								//this is not enough chars
								//display a toast to notify user
								Toast.makeText(yearBuilt.getContext(), "Not enough numbers to make a valid year. Value set to current year.", Toast.LENGTH_LONG).show();

								//get present year
								Calendar c = Calendar.getInstance(); 
								int presentYear=c.get(Calendar.YEAR);

								//set it to the present year
								yearBuilt.setText(String.valueOf(presentYear));
							}

							//validate the year
			        		validateStructureAge();

							//check to see if data is different
							int testInt = Integer.parseInt(yearBuilt.getText().toString());

							if(mYearBuilt != testInt){
								//data is not the same
								
								//set flag that data has changed
								mInfoHasChanged = true;

								//copy data to local variable
								mYearBuilt = testInt;

								//notify parent that there is data to save
								mParent.notifyOfNewData();
							}else{
								//set flag that data has not changed
								mInfoHasChanged = false;
							}
						}else{
							//set this view as the active view
							mFocusedView = R.id.edittext_year_built;
						}
					
					}
				});
		
		
		..............................................................................................................

	@Override
	public void onSaveInstanceState(Bundle outState){
		
		..............................................................................................................
		outState.putInt(PROPERTY_INFO_YEAR_BUILT, mYearBuilt);
		..............................................................................................................
		
		super.onSaveInstanceState(outState);
		
	}
	
	@Override
	public void onPause(){
		super.onPause();
		
		//all data changed before this method was invoked has already
		//been saved to local variables and the flag has been set to 
		//send it to the parent.
		//we just need to extract the data from the focused view to 
		//validate it and determine what data needs to be sent to the 
		//parent.
		
		//check the last focused view for input
		switch(mFocusedView){
		
			..............................................................................................................
			case R.id.edittext_year_built:{
				//validate data
				
				//make sure the field isn't blank or missing numbers.  if it is change it to a default value

				//check to see if it is empty first
				if(yearBuilt.getText().toString().contentEquals("")){
					//edit text is blank
					//notify user this can't be blank
					Toast.makeText(yearBuilt.getContext(), "This value cannot be blank.  Value set to current year.", Toast.LENGTH_LONG).show();

					//get present year
					Calendar c = Calendar.getInstance(); 
					int presentYear=c.get(Calendar.YEAR);

					//set it to the present year
					yearBuilt.setText(String.valueOf(presentYear));

				}else 
				//check to see that there are enough characters for a year

				if (yearBuilt.length() < 4){
					//this is not enough chars
					//display a toast to notify user
					Toast.makeText(yearBuilt.getContext(), "Not enough numbers to make a valid year. Value set to current year.", Toast.LENGTH_LONG).show();

					//get present year
					Calendar c = Calendar.getInstance(); 
					int presentYear=c.get(Calendar.YEAR);

					//set it to the present year
					yearBuilt.setText(String.valueOf(presentYear));
				}

				//validate the year
        		validateStructureAge();

				//check to see if data is different
				int testInt = Integer.parseInt(yearBuilt.getText().toString());

				if(mYearBuilt != testInt){
					//data is not the same
					
					//set flag that data has changed
					mInfoHasChanged = true;

					//copy data to local variable
					mYearBuilt = testInt;

					//notify parent that there is data to save
					mParent.notifyOfNewData();
				}else{
					//set flag that data has not changed
					mInfoHasChanged = false;
				}
				
				break;
			}
		}
		
		//has data changed
		if(mInfoHasChanged){
			//bundle to hold data
			Bundle extras = new Bundle();
			
			//Bundle all data and return it to the activity
			..............................................................................................................
			extras.putInt(PROPERTY_INFO_YEAR_BUILT, mYearBuilt);
			..............................................................................................................
			
			//send the data to the parent
			mListener.onPropertyInfoChanged(extras);
		}
	}
	
	private void validateStructureAge(){
		//ensure input year is:
		//1. not blank.  if it is, set it to the age of the roof.  set the strutureAge spinner to the decided age.
		//     notify user that this value cannot be left blank and it was set to the age of the roof.  update class variables.
		
		//2. contains enough characters. if it doesn't, set it to the age of the roof.  set the strutureAge spinner to the 
		//     decided age.  notify user that this value did not contain enough numbers and it was set to the age 
		//     of the roof.  update class variables.
		
		//3. not past present year.  if it is, set it to the age of the roof.  set the strutureAge spinner to the 
		//     decided age.  notify user that this value cannot be past the present year and it was set to the age 
		//     of the roof.  update class variables.
		
		//4. not before the age of the roof.  if it is, set it to the age of the roof.  set the strutureAge spinner to the 
		//     decided age.  notify user that the structure cannot be younger than the roof and it was set to the age 
		//     of the roof.   update class variables.
		
		//get the year value in the edittext
		String enteredYear = yearBuilt.getText().toString();
		
		//get present year
		Calendar c = Calendar.getInstance(); 
		int presentYear = c.get(Calendar.YEAR);
		
		//get the age of the roof
		int ageOfRoof = Integer.parseInt((String)roofAgeYears.getSelectedItem());
		
		
		//*************************************************************************************
		//1
		
		//check to see if it is empty
    	if(enteredYear.contentEquals("") ){
    		//edit text is blank
    		
    		//calculate the year built
    		int newYearBuilt = presentYear - ageOfRoof;
    		
    		//set the enteredYear to the year roof was made
	   		 enteredYear = String.valueOf(newYearBuilt);
    		
    		//set edittext to the new year built
    		yearBuilt.setText(enteredYear);
    		
    		//set the strutureAge spinner to the age of the roof
    		//loop through to find the right item
   		 	for (int i = 0 ; i < structureYears.getCount() ; i++){
	   			if(Integer.parseInt((String)structureYears.getItemAtPosition(i)) == ageOfRoof){
	   				//select this item
	   				structureYears.setSelection(i, true);
	   				break;
	   			}
   		 	}
   		 	
			//notify user this can't be blank
			Toast.makeText(yearBuilt.getContext(), "This value cannot be blank.  Value set to soonest year.", Toast.LENGTH_LONG).show();
			
    	}else 
    		//*************************************************************************************
    		//2
    		//it is not blank.
    		//check that there are enough characters for a year
    		
    		if (yearBuilt.length() < 4){
        		//this is not enough chars
        		
        		//calculate the year built
        		int newYearBuilt = presentYear - ageOfRoof;
        		
        		//set the enteredYear to the year roof was made
   	   		 	enteredYear = String.valueOf(newYearBuilt);
        		
        		//set it to the new year built
        		yearBuilt.setText(enteredYear);
        		
        		//set the strutureAge spinner to the age of the roof
        		//loop through to find the right item
       		 	for (int i = 0 ; i < structureYears.getCount() ; i++){
    	   			if(Integer.parseInt((String)structureYears.getItemAtPosition(i)) == ageOfRoof){
    	   				//select this item
    	   				structureYears.setSelection(i, true);
    	   				break;
    	   			}
       		 	}
        		
        		//notify user this can't be blank
        		Toast.makeText(yearBuilt.getContext(), "There were not enough numbers to make a valid year.  Value set to soonest year.", Toast.LENGTH_LONG).show();
        		
    	}else 
    		//*************************************************************************************
    		//3
    		//it is not empty
    		//it has enough characters
    		//check to see that it is not in the future
    		
    		if(Integer.parseInt(enteredYear) > presentYear){
    			//the entered year is in the future
    			
    			//calculate the year built
        		int newYearBuilt = presentYear - ageOfRoof;
        		
        		//set the enteredYear to the year roof was made
   	   		 	enteredYear = String.valueOf(newYearBuilt);
        		
        		//set it to the new year built
        		yearBuilt.setText(enteredYear);
        		
        		//set the strutureAge spinner to the age of the roof
        		//loop through to find the right item
       		 	for (int i = 0 ; i < structureYears.getCount() ; i++){
    	   			if(Integer.parseInt((String)structureYears.getItemAtPosition(i)) == ageOfRoof){
    	   				//select this item
    	   				structureYears.setSelection(i, true);
    	   				break;
    	   			}
       		 	}
       		 	
    			//display a toast informing the user of the correction
   			 Toast.makeText(yearBuilt.getContext(), "The entered year is after the present year. "
   			 		+ "The present year has been substituted.", Toast.LENGTH_LONG).show();
    		}else
    			//*************************************************************************************
    			//4
    			//it is not empty
    			//it has enough characters
    			//it is not in the future
    			//check that age of structure is not younger than the roof
    			if((presentYear - Integer.parseInt(enteredYear)) < ageOfRoof){
    				
    				//calculate the year built
            		int newYearBuilt = presentYear - ageOfRoof;
            		
            		//set the enteredYear to the year roof was made
       	   		 	enteredYear = String.valueOf(newYearBuilt);
            		
            		//set it to the new year built
            		yearBuilt.setText(enteredYear);
            		
            		//set the strutureAge spinner to the age of the roof
            		//loop through to find the right item
           		 	for (int i = 0 ; i < structureYears.getCount() ; i++){
        	   			if(Integer.parseInt((String)structureYears.getItemAtPosition(i)) == ageOfRoof){
        	   				//select this item
        	   				structureYears.setSelection(i, true);
        	   				break;
        	   			}
           		 	}
           		 	
        			//display a toast informing the user of the correction
       			 Toast.makeText(yearBuilt.getContext(), "The age of the structure cannot be less than the age of the roof. "
       			 		+ "Value set to the age of the roof.", Toast.LENGTH_LONG).show();
    			}else{
    				//everything checked out, so change the value of the spinner to the input value
    				
    				//calculate the age of the structure from given info
    				int yearsOld = presentYear - Integer.parseInt(enteredYear);
    				
    				//set the strutureAge spinner to the age of the entered year
            		//loop through to find the right item
           		 	for (int i = 0 ; i < structureYears.getCount() ; i++){
        	   			if(Integer.parseInt((String)structureYears.getItemAtPosition(i)) == yearsOld){
        	   				//select this item
        	   				structureYears.setSelection(i, true);
        	   				break;
        	   			}
           		 	}
    			}
		
	}
	
	private void setupUI(){
	
		//set up all the views with the local data
		..............................................................................................................
		yearBuilt.setText(String.valueOf(mYearBuilt));
		..............................................................................................................
	
	}

}
