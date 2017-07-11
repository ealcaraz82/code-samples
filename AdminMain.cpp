#define WIN32_LEAN_AND_MEAN //standard for Windows
#define _WIN32_DCOM
#include <windows.h> //standard for Windows
#include <mmsystem.h> //Windows Multimedia
#include <winbase.h> //used for the performance timer

#include <fstream.h> //used for text output to file
#include <string.h> //String functions used in debugging
#include <assert.h> //debugging support

#include <list>  //linked list support
using namespace std;

#include <dsound.h> //DirectSound8
#include <d3d8.h> //Direct3D8
#include <d3dx8.h> //Direct3DX8
#include <dxfile.h> //DirectXFile
#define DIRECTINPUT_VERSION		0x0800
#include <dinput.h> //DirectInput8
#include <dplay8.h> //DirectPlay8
#include <dxutil.h> //DirectX Utility Library

#define SafeRelease(x) if (x) { x->Release(); x = NULL; }

#include "AdminResources.h"

#include "IndependantGlobals.h"

#include "MWXL.h"
#include "MWVB.h"
#include "RWPB.h"
#include "FileIO.h"
#include "AdminState.h"

#include "Globals.h"

#include "ServerIPAddress.h"

#include "MyDirectPlay.h"

#include "MyLoginStatus.h"
#include "MyConnectionStatus.h"
#include "MyGameMessageLibrary.h"
#include "MySystemMessageLibrary.h"

#include "MultithreadFunctions.h"

#include "AdminMain.h"

//client control definitions
#include "MyButton.h"
#include "MyLabel.h"
#include "MyTextbox.h"

//Main window definitions
#include "MainFrameWindow.h"
#include "ClientWindow.h"

//other window definitions
#include "AdminLoginWindow.h"
#include "AdminConnectWindow.h"

#include "Debug.h"

int WINAPI WinMain(	HINSTANCE hInstance,
					HINSTANCE hPrevInstance,
					LPSTR     lpCmdLine,
					int       nCmdShow)
{
 	MSG msg;

	hGlobalInstance = hInstance;

	//initialize the application, bail out if it fails
	//create main window here
	HRESULT hret;
	hret = MyMainFrameWindow.InitializeWindow( hInstance,
										MAKEINTRESOURCE(IDR_ADMINMENU),
										MAKEINTRESOURCE(IDI_ADMINICON),
										"The World Online - Administrative Client (offline)",
										800,
										600,
										100,
										100);
	if(hret != S_OK)
	{ 
		return false;
	}


	hret = MyMainFrameWindow.Show();
	if(hret != S_OK)
	{ 
		return false;
	}

	//message loop handler
	while (IsNotDone)
	{
		if (PeekMessage(&msg,NULL,0,0,PM_REMOVE))
		{
			if (!TranslateMDISysAccel(*MyClientWindow.WindowHWnd(), &msg))
			{
				TranslateMessage(&msg);
				DispatchMessage(&msg);
			}
		}
		else
		{	//main loop
			switch (g_AdminState.QueryAdminState())
			{
				case ADMIN_INITIALIZE:
					{
						g_AdminState.ChangeAdminState (ADMIN_MENUCREATION);
						break;
					}

				case ADMIN_MENUCREATION:
					{	
						break;
					}

				case ADMIN_TERMINATE:
					{
						PostQuitMessage(0);
						IsNotDone = FALSE;
						break;
					}
			}
		}
	}

	Cleanup();

	return (msg.wParam);

}

//the Frame window procedure
LRESULT CALLBACK MainFrameProc(HWND hWnd, unsigned uMsg, WPARAM wParam, LPARAM lParam)
{
	HRESULT hret;
	hret = MyMainFrameWindow.WindowHWnd(hWnd);
	if (hret != S_OK)
	{
		return 0L;
	}

	switch (uMsg)
	{
		case WM_CREATE:
			{
				HRESULT hret;

				hret = MyClientWindow.InitializeWindow(hGlobalInstance);
				if (hret != S_OK)
				{
					return 0L;
				}

				hret = MyClientWindow.Show();
				if (hret != S_OK)
				{
					return 0L;
				}
			}
		case WM_COMMAND:
			{

				WORD wID = LOWORD(wParam);

				switch(wID)
				{
					case MNU_FILE_LOGIN:
						{
							//temp - will be replaced by a thread
							bool bret;
							bret = MyAdminlogin.IsAlreadyRunning();
							if(!bret)
							{
								MyAdminlogin.InitializeWindow(hGlobalInstance);
								MyAdminlogin.Show();
							}
							break;
						}

					case MNU_FILE_LOGOUT:
						{
							MyMainFrameWindow.Logout_Successful();
							break;
						}

					case MNU_FILE_CONNECTTOSERVER:
						{
							bool bret;
							bret = MyAdminConnect.IsAlreadyRunning();
							if(!bret)
							{
								MyAdminConnect.InitializeWindow(hGlobalInstance);
								MyAdminConnect.Show();
							}
							break;
						}

					case MNU_FILE_DISCONNECTFROMSERVER:
						{
							MyMainFrameWindow.Disconnect_Successful();
							break;
						}

					case MNU_FILE_SETSERVERIPADDRESS:
						{
							//provide a way to set the server's IP address manually
							MyServerIP.SetIPAddress();
							break;
						}

					case MNU_FILE_EXIT:
						{
							g_AdminState.ChangeAdminState (ADMIN_TERMINATE);
							break;
						}
						.............................................................................................
				}

				break;
			}

		case WM_CLOSE:
			{
				g_AdminState.ChangeAdminState (ADMIN_TERMINATE);
				break;
			}

		default:
			{
				return DefFrameProc(hWnd, *MyClientWindow.WindowHWnd(), uMsg, wParam, lParam);
				break;
			}
	}
	return 0L;
}


//the Client window procedure
LRESULT CALLBACK ClientWindowProc(HWND hWnd, unsigned uMsg, WPARAM wParam, LPARAM lParam)
{

	HRESULT hret;
	hret = MyClientWindow.WindowHWnd(hWnd);
	if (hret != S_OK)
	{
		return 0L;
	}

	switch (uMsg)
	{
		case WM_CLOSE:
			{
				break;
			}

		default:
			{
				return DefMDIChildProc(hWnd, uMsg, wParam, lParam);
				break;
			}
	}

	return 0L;
}

//the AdminLogin window procedure
LRESULT CALLBACK AdminLoginWindowProc(HWND hWnd, unsigned uMsg, WPARAM wParam, LPARAM lParam)
{

	MyAdminlogin.WindowHWnd(hWnd);

	char lpUsername[31];
	char lpPassword[31];
    WORD cchPassword;
    WORD cchUsername;

	//process messages
	switch (uMsg)
	{
		case WM_ACTIVATE:
			{
				switch(LOWORD(wParam))
				{
					case WA_ACTIVE:
						{
							MyMainFrameWindow.Window_Activated(hWnd);
						}
					case WA_CLICKACTIVE:
						{
							MyMainFrameWindow.Window_Activated(hWnd);
						}
					case WA_INACTIVE:
						{
							MyMainFrameWindow.Window_Deactivated();
						}
				}

				return 0;
				break;
			}
		case WM_CREATE:
			{
				MyAdminlogin.InitializeControls(hGlobalInstance);
				return 0;
				break;
			}
		case WM_COMMAND:
			{
                switch (LOWORD(wParam)) 
                { 
                    case BTN_OK:
                        {
							//test for click
							switch (HIWORD(wParam))
							{
								case BN_CLICKED:
									{

										//disable the box
                                        EnableWindow(*MyClientWindow.WindowHWnd(), FALSE);

										//store character values
										SendDlgItemMessage( hWnd,
															TXT_USERNAME,
															WM_GETTEXT,
															(WPARAM) 31,
															(LPARAM) lpUsername);

										SendDlgItemMessage( hWnd,
															TXT_PASSWORD,
															WM_GETTEXT,
															(WPARAM) 31,
															(LPARAM) lpPassword);

										//send info and wait for response
										bool ValidAccount = MyAdminlogin.Log_In(lpUsername, lpPassword);
										
										if (ValidAccount)
										{
											//enable all necessary controls on main menu
											MyMainFrameWindow.Login_Successful();

											//display a message to user that login was
											//successful
											MessageBox(hWnd, "Your login was successful! Welcome.", 
												"Connected", MB_OK|MB_ICONINFORMATION|MB_APPLMODAL);

											//unload this window
											SendMessage(*MyClientWindow.WindowHWnd(),
														WM_MDIDESTROY,
														(WPARAM) (HWND) hWnd,
														0);

											MyAdminlogin.Release();
										}
										else
										{
											//display message to the user that they
											//have an invalid account

											//ask for login count
											int Count = MyAdminlogin.LoginCount();
											if(Count < 3)
											{
												//inform the user of number of attempts remaining before app unloads
												LPSTR MessagePartA = "Your login was unsuccessful! Please try again. You have ";
												LPSTR MessagePartB = " login attempt(s) remaining.";

												LPSTR lpszRemain = new char[2];
												LPSTR CompleteMessage = new char[87];
												
												int iRemain = (3 - Count);
												_itoa(iRemain, lpszRemain, 10);

												strcpy(CompleteMessage, MessagePartA);
												strcat(CompleteMessage, lpszRemain);
												strcat(CompleteMessage, MessagePartB);

												MessageBox(hWnd, CompleteMessage, "Attempts Remaining", 
													MB_OK|MB_ICONHAND|MB_APPLMODAL);

												delete lpszRemain;
												delete CompleteMessage;

												//blank the user info on the form
												SendDlgItemMessage( hWnd,
																	TXT_USERNAME,
																	WM_SETTEXT,
																	(WPARAM) 0,
																	(LPARAM)(LPCTSTR) "");

												SendDlgItemMessage( hWnd,
																	TXT_PASSWORD,
																	WM_SETTEXT,
																	(WPARAM) 0,
																	(LPARAM)(LPCTSTR) "");

												//enable the box
												EnableWindow(*MyClientWindow.WindowHWnd(), TRUE);
											}
											else if (Count >= 3)
											{
												//tell user that login is unsuccessful
												MessageBox(hWnd, "Your login was unsuccessful! This application will now exit.", 
												"Sorry...", MB_OK|MB_ICONSTOP|MB_APPLMODAL);

												//unload this application
												SendMessage(*MyMainFrameWindow.WindowHWnd(),
															WM_CLOSE,
															(WPARAM) 0,
															(LPARAM) 0);
											}
										}
										break;
									}
							}

                            break;
                        }
                    case BTN_CANCEL:
                        {
							//test for click
							switch (HIWORD(wParam))
							{
								case BN_CLICKED:
									{
										MyAdminlogin.Cancel_Clicked();
										break;
									}
							}

                            break;
                        }
                    case TXT_USERNAME:
                        {
							//test for change
							switch (HIWORD(wParam))
							{
								case EN_CHANGE:
									{
										// Get number of characters. 
										cchUsername = (WORD) SendDlgItemMessage(hWnd,
																				TXT_USERNAME,
																				EM_GETLINE,
																				(WPARAM) 0,
																				(LPARAM) lpUsername);

										cchPassword = (WORD) SendDlgItemMessage(hWnd,
																				TXT_PASSWORD,
																				EM_GETLINE,
																				(WPARAM) 0,
																				(LPARAM) lpPassword);

										MyAdminlogin.Username_Changed(cchUsername, cchPassword);
										break;
									}
							}
                            break;
                        }
                    case TXT_PASSWORD:
                        {
							//test for change
							switch (HIWORD(wParam))
							{
								case EN_CHANGE:
									{

                                        // Get number of characters. 
                                        cchUsername = (WORD) SendDlgItemMessage(hWnd,
                                                                                TXT_USERNAME,
                                                                                EM_GETLINE,
                                                                                (WPARAM) 0,
                                                                                (LPARAM) lpPassword);

                                        cchPassword = (WORD) SendDlgItemMessage(hWnd,
                                                                                TXT_PASSWORD,
                                                                                EM_GETLINE,
                                                                                (WPARAM) 0,
                                                                                (LPARAM) lpPassword);

										MyAdminlogin.Password_Changed(cchUsername, cchPassword);
										break;
									}
							}

                            break;
						}
				}

				break;
			}
		
		.................................................................................................................

		case WM_CLOSE:
			{
				//should send message to the client window to destroy this window
				SendMessage(*MyClientWindow.WindowHWnd(),
							WM_MDIDESTROY,
							(WPARAM) (HWND) hWnd,
							0);
				MyAdminlogin.Release();

				return 0;
				break;
			}

		default:
			{
				return DefMDIChildProc(hWnd, uMsg, wParam, lParam);
				break;
			}
	}
	return 0L;
}

//the AdminLogin window procedure
LRESULT CALLBACK AdminConnectWindowProc(HWND hWnd, unsigned uMsg, WPARAM wParam, LPARAM lParam)
{

	MyAdminConnect.WindowHWnd(hWnd);

	char lpUsername[31];
	char lpPassword[31];
    WORD cchPassword;
    WORD cchUsername;

	//process messages
	switch (uMsg)
	{
		case WM_ACTIVATE:
			{
				switch(LOWORD(wParam))
				{
					case WA_ACTIVE:
						{
							MyMainFrameWindow.Window_Activated(hWnd);
						}
					case WA_CLICKACTIVE:
						{
							MyMainFrameWindow.Window_Activated(hWnd);
						}
					case WA_INACTIVE:
						{
							MyMainFrameWindow.Window_Deactivated();
						}
				}

				return 0;
				break;
			}
		case WM_CREATE:
			{
				MyAdminConnect.InitializeControls(hGlobalInstance);
				return 0;
				break;
			}
		case WM_COMMAND:
			{
                switch (LOWORD(wParam)) 
                { 
                    case BTN_OK:
                        {
							//test for click
							switch (HIWORD(wParam))
							{
								case BN_CLICKED:
									{
										//store character values
										SendDlgItemMessage( hWnd,
															TXT_USERNAME,
															WM_GETTEXT,
															(WPARAM) 31,
															(LPARAM) lpUsername);

										SendDlgItemMessage( hWnd,
															TXT_PASSWORD,
															WM_GETTEXT,
															(WPARAM) 31,
															(LPARAM) lpPassword);

										//send info and wait for response
										bool ValidAccount = MyAdminConnect.Connect(lpUsername, lpPassword);
										
										if (ValidAccount)
										{
											//enable all necessary controls on main menu
											MyMainFrameWindow.Connect_Successful();

											//display a message to user that connection was
											//successful
											MessageBox(hWnd, "Your connection was successful! Welcome.", 
												"Connected", MB_OK|MB_ICONINFORMATION|MB_APPLMODAL);

											//unload this window
											SendMessage(*MyClientWindow.WindowHWnd(),
														WM_MDIDESTROY,
														(WPARAM) (HWND) hWnd,
														0);

											MyAdminConnect.Release();
										}
										else
										{
											//display message to the user that they
											//have an invalid account

											//ask for connection count
											int Count = MyAdminConnect.ConnectCount();
											if(Count < 3)
											{
												//inform the user of number of attempts remaining before app unloads
												LPSTR MessagePartA = "Your connection was unsuccessful! Please try again. You have ";
												LPSTR MessagePartB = " connection attempt(s) remaining.";

												LPSTR lpszRemain = new char[2];
												LPSTR CompleteMessage = new char[97];
												
												int iRemain = (3 - Count);
												_itoa(iRemain, lpszRemain, 10);

												strcpy(CompleteMessage, MessagePartA);
												strcat(CompleteMessage, lpszRemain);
												strcat(CompleteMessage, MessagePartB);

												MessageBox(hWnd, CompleteMessage, "Attempts Remaining", 
													MB_OK|MB_ICONHAND|MB_APPLMODAL);

												delete lpszRemain;
												delete CompleteMessage;
												//blank the user info on the form
											}
											else if (Count >= 3)
											{
												//tell user that connection is unsuccessful
												MessageBox(hWnd, "Your connection was unsuccessful! This application will now exit.", 
												"Sorry...", MB_OK|MB_ICONSTOP|MB_APPLMODAL);

												//unload this application
												SendMessage(*MyMainFrameWindow.WindowHWnd(),
															WM_CLOSE,
															(WPARAM) 0,
															(LPARAM) 0);
											}
										}
										break;
									}
							}

                            break;
                        }
                    case BTN_CANCEL:
                        {
							//test for click
							switch (HIWORD(wParam))
							{
								case BN_CLICKED:
									{
										MyAdminConnect.Cancel_Clicked();
										break;
									}
							}

                            break;
                        }
                    case TXT_USERNAME:
                        {
							//test for change
							switch (HIWORD(wParam))
							{
								case EN_CHANGE:
									{
										// Get number of characters. 
										cchUsername = (WORD) SendDlgItemMessage(hWnd,
																				TXT_USERNAME,
																				EM_GETLINE,
																				(WPARAM) 0,
																				(LPARAM) lpUsername);

										cchPassword = (WORD) SendDlgItemMessage(hWnd,
																				TXT_PASSWORD,
																				EM_GETLINE,
																				(WPARAM) 0,
																				(LPARAM) lpPassword);

										MyAdminConnect.Username_Changed(cchUsername, cchPassword);
										break;
									}
							}
                            break;
                        }
                    case TXT_PASSWORD:
                        {
							//test for change
							switch (HIWORD(wParam))
							{
								case EN_CHANGE:
									{

                                        // Get number of characters. 
                                        cchUsername = (WORD) SendDlgItemMessage(hWnd,
                                                                                TXT_USERNAME,
                                                                                EM_GETLINE,
                                                                                (WPARAM) 0,
                                                                                (LPARAM) lpPassword);

                                        cchPassword = (WORD) SendDlgItemMessage(hWnd,
                                                                                TXT_PASSWORD,
                                                                                EM_GETLINE,
                                                                                (WPARAM) 0,
                                                                                (LPARAM) lpPassword);

										MyAdminConnect.Password_Changed(cchUsername, cchPassword);
										break;
									}
							}

                            break;
						}
				}

				break;
			}
		
		...................................................................................................................

		case WM_CLOSE:
			{
				//should send message to the client window to destroy this window
				SendMessage(*MyClientWindow.WindowHWnd(),
							WM_MDIDESTROY,
							(WPARAM) (HWND) hWnd,
							0);
				MyAdminConnect.Release();

				return 0;
				break;
			}

		default:
			{
				return DefMDIChildProc(hWnd, uMsg, wParam, lParam);
				break;
			}
	}
	return 0L;
}

HRESULT WINAPI DirectPlayMessageHandlerClient(PVOID pvUserContext, DWORD dwMessageId, PVOID pMsgBuffer)
{
    HRESULT     hr = S_OK;
    switch (dwMessageId)
    {
        case DPN_MSGID_RECEIVE:
        {
			//set security attributes for new thread
			SECURITY_ATTRIBUTES ThreadAttributes;
			ThreadAttributes.nLength = sizeof(SECURITY_ATTRIBUTES);
			ThreadAttributes.bInheritHandle = TRUE;
			ThreadAttributes.lpSecurityDescriptor = NULL;

			EnterCriticalSection(&g_csMessageReceived);

			//save message and pass pointer
			void* pvToThread = MySystemMessageLibrary.Add_Message_DPNMSG_RECEIVE(pMsgBuffer);

			//create thread to handle message
			HANDLE NewThread = CreateThread(&ThreadAttributes,
											(DWORD)1024,
											MessageReceived,
											pvToThread,
											0,
											NULL);
			if (NewThread == NULL)
			{
				//failed creating thread
			}

			CloseHandle(NewThread);

			LeaveCriticalSection(&g_csMessageReceived);

            break;
        }

		case DPN_MSGID_ENUM_HOSTS_RESPONSE:
		{
			//store host information
			EnterCriticalSection(&g_csEnumeratingHosts);

            PDPNMSG_ENUM_HOSTS_RESPONSE     pEnumHostsResponseMsg;
            const DPN_APPLICATION_DESC*     pAppDesc;
			IDirectPlay8Address* pTempHostAddress;

            pEnumHostsResponseMsg = (PDPNMSG_ENUM_HOSTS_RESPONSE) pMsgBuffer;
            pAppDesc = pEnumHostsResponseMsg->pApplicationDescription;

            // Insert each host response if it isn't already present
            if( MyDP.IsHostPresent(pAppDesc->guidInstance) == S_OK)
                {
                    // This host is already in the list
					pAppDesc = NULL;

					break;
                }

            // Copy the Host Address
            if(FAILED(pEnumHostsResponseMsg->pAddressSender->Duplicate(&pTempHostAddress)))
            {
				pTempHostAddress = NULL;
				pAppDesc = NULL;
				pEnumHostsResponseMsg = NULL;

				break;
            }

			//add a host to the list
			MyDP.AddHost(pTempHostAddress, *pAppDesc);

            pTempHostAddress = NULL;
			pAppDesc = NULL;
			pEnumHostsResponseMsg = NULL;

			LeaveCriticalSection(&g_csEnumeratingHosts);

			break;
		}

		.......................................................................................................

		case DPN_MSGID_TERMINATE_SESSION:
		{
			
            PDPNMSG_TERMINATE_SESSION   pTermSessionMsg;

            pTermSessionMsg = (PDPNMSG_TERMINATE_SESSION) pMsgBuffer;

		........................................................................................................
			
			break;
		}

		.........................................................................................................
	}

    return hr;
}


HRESULT InitDI8(HINSTANCE hInstance, HWND hWnd)
{
	HRESULT hRet;
	//initialize direct input

	hRet = DirectInput8Create(hInstance,DIRECTINPUT_VERSION, IID_IDirectInput8, (void**)&g_pdi8, NULL);
	if (hRet != DI_OK)
	{
		ErrStr = Err_DirectInputCreate;
		return S_FALSE;
	}

	//lpDIDKeyboard initialize

	hRet = g_pdi8->CreateDevice(GUID_SysKeyboard, &g_pdiKeyboard, NULL);
	if (hRet != DI_OK)
	{
		ErrStr = Err_InputDeviceCreate;
		return S_FALSE;
	}

	//set keyboard data format
	hRet = g_pdiKeyboard->SetDataFormat(&c_dfDIKeyboard);
	if (hRet != DI_OK)
	{
		ErrStr = Err_DeviceDataFormat;
		return S_FALSE;
	}

	//set keyboard cooperative level
	hRet = g_pdiKeyboard->SetCooperativeLevel(hWnd, DISCL_FOREGROUND|DISCL_EXCLUSIVE);
	if (hRet != DI_OK)
	{
		ErrStr = Err_DeviceCoop;
		return S_FALSE;
	}

	//acquire keyboard

	//lpDIDMouse initialize

	hRet = g_pdi8->CreateDevice(GUID_SysMouse,&g_pdiMouse, NULL);
	if (hRet != DI_OK)
	{
		ErrStr = Err_InputDeviceCreate;
		return S_FALSE;
	}

	//set mouse data format
	hRet = g_pdiMouse->SetDataFormat(&c_dfDIMouse);
	if (hRet != DI_OK)
	{
		ErrStr = Err_DeviceDataFormat;
		return S_FALSE;
	}

	//set mouse cooperative level
	hRet = g_pdiMouse->SetCooperativeLevel(hWnd, DISCL_FOREGROUND|DISCL_EXCLUSIVE);
	if (hRet != DI_OK)
	{
		ErrStr = Err_DeviceCoop;
		return S_FALSE;
	}

	//acquire mouse

	return S_OK;
}

HRESULT InitDS8(HWND hWnd)
{
	HRESULT hRet;
	//initialize direct sound

	hRet = DirectSoundCreate8(NULL,&g_pds8,NULL);
	if ( hRet != DS_OK)
	{
		ErrStr = Err_DirectSoundCreate;
		return S_FALSE;
	}

	//set the cooperative level of direct sound

	hRet = g_pds8->SetCooperativeLevel (hWnd,DSSCL_NORMAL);
	if (hRet != DS_OK)
	{
		ErrStr = Err_Coop;
		return S_FALSE;
	}

	return S_OK;
}

HRESULT SetupTimer(void)
{
	//will need to move the time update loop out of here
	//keep the check for presence code here

	//query the performance timer

	if(QueryPerformanceFrequency((LARGE_INTEGER *) &g_llTimerFrequency))
	{	//set up the performance timer
	
		//set the frame rate

		g_dwTimeCount = (unsigned long)(g_llTimerFrequency/30);
		QueryPerformanceCounter((LARGE_INTEGER *) &g_llNextTime);
		g_dTimeScale = 1.0/g_llTimerFrequency;
	}
	else
	{	//no performance timer!?!
		ErrStr = Err_PerfTimer;
		return S_FALSE;
	}

	//save the last frame

	g_llLastTime = g_llNextTime;

	return S_OK;
}

void Cleanup(void)
{

	Cleanup_DirectPlay();

	//release direct 3d
	SafeRelease(g_pd3d);
	SafeRelease(g_pd3dDevice);

	//release direct input
	if (g_pdiKeyboard)
	{
		g_pdiKeyboard->Unacquire();
		g_pdiKeyboard->Release();
		g_pdiKeyboard = NULL;
	}

	if (g_pdiMouse)
	{
		g_pdiMouse->Unacquire();
		g_pdiMouse->Release();
		g_pdiMouse = NULL;
	}
	
	SafeRelease(g_pdi8);
	
	//release direct sound
	SafeRelease(g_pds8);

	Cleanup_CriticalSections();

	if(ErrStr)
	{
		MessageBox(*MyMainFrameWindow.WindowHWnd(), ErrStr, "There was an Error", MB_OK|MB_ICONERROR|MB_SYSTEMMODAL);
	}

	MyMainFrameWindow.Release();
	MyClientWindow.Release();
}

void Cleanup_DirectPlay(void)
{
    // Shutdown DirectPlay
    if(g_pDPClient)
       g_pDPClient->Close(0);

	//destroy server info
	MyDP.ReleaseHostInfo();
    MyDP.ReleaseDeviceAddress();
	MyDP.ReleaseHostAddress();
    SafeRelease(g_pDPClient);
}

void Cleanup_CriticalSections(void)
{
	DeleteCriticalSection(&g_csMessageReceived);
	DeleteCriticalSection(&g_cs_HDLR_MessageReceived);
	DeleteCriticalSection(&g_csEnumeratingHosts);
	DeleteCriticalSection(&g_csConnectComplete);
	DeleteCriticalSection(&g_csSendComplete);
	DeleteCriticalSection(&g_csApplicationDesc);
	DeleteCriticalSection(&g_csServerInfo);
	DeleteCriticalSection(&g_csTerminateSession);
	DeleteCriticalSection(&g_csAsycOpComplete);
	DeleteCriticalSection(&g_csReturnBuffer);

}