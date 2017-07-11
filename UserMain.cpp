#define WIN32_LEAN_AND_MEAN //standard for Windows
#include <windows.h> //standard for Windows
#include <mmsystem.h> //Windows Multimedia
#include <winbase.h> //used for the performance timer

#include <dsound.h> //DirectSound8

#include <d3d8.h> //Direct3D8
#include <d3dx8.h> //Direct3DX8

#define DIRECTINPUT_VERSION		0x0800
#include <dinput.h> //DirectInput8

#include <fstream.h> //used for error text output to file
#include <string.h> //String functions used in debugging
#include <assert.h> //debugging support
#include <stdio.h> //TEMP UNDECIDED

#define SafeRelease(x) if (x) { x->Release(); x = NULL; }

//local defines go here
#include "UserMain.h"

int WINAPI WinMain(	HINSTANCE hInstance,
					HINSTANCE hPrevInstance,
					LPSTR     lpCmdLine,
					int       nCmdShow)
{
 	MSG msg;
	HWND hWnd1 = NULL;

	//for single commands
	g_CmdLineString = lpCmdLine;

	switch (*g_CmdLineString)
	{
	case 'w':
		{
			g_AppState.ChangeAppWindowState (APP_WINDOWED);
			break;
		}
	case 'W':
		{
			g_AppState.ChangeAppWindowState (APP_WINDOWED);
			break;
		}

	case 'f':
		{
			g_AppState.ChangeAppWindowState (APP_FULLSCREEN);
			break;
		}
	case 'F':
		{
			g_AppState.ChangeAppWindowState (APP_FULLSCREEN);
			break;
		}
	}

    .......................................................................................................................

	//initialize the application, bail out if it fails
	
	if ( !Initialize(hInstance, nCmdShow, hWnd1))
	{
		Cleanup();
		return FALSE;
	}

	//message loop handler
	BOOL notDone = TRUE;

	while (notDone)
	{
		if (PeekMessage(&msg,NULL,0,0,PM_REMOVE))
		{
			TranslateMessage(&msg);
			DispatchMessage(&msg);
		}
		else
		{	//game loop
			switch (g_AppState.QueryAppState())
			{
				case APP_INITIALIZE: //app has already been initialized so change state
					{
						g_AppState.ChangeAppState (APP_MENUCREATION);
						break;
					}

				case APP_MENUCREATION:
					{	
						MainMenuCreate(g_pMainMenu);
						g_pMainMenu->CreateRoot ("Main/n");
						g_pMainMenu->CreateSubMenu(0,R_CHILD,"New Game Options/n");
						g_pMainMenu->CreateSubMenu(0,R_CHILD,"Load Game Options/n");
						g_pMainMenu->CreateSubMenu(0,R_CHILD,"Options Options/n");
						g_pMainMenu->CreateSubMenu(0,R_CHILD,"View Credits/n");
						g_pMainMenu->CreateSubMenu(3,R_CHILD,"Video Options/n");
						g_pMainMenu->CreateSubMenu(3,R_CHILD,"Audio Options/n");
						g_pMainMenu->CreateSubMenu(3,R_CHILD,"Control Options/n");
						g_pMainMenu->CreateSubMenu(3,R_CHILD,"UI Options/n");
						g_pMainMenu->CreateMenuItem ("Main/n",
													NULL,
													NULL,
													D3DXVECTOR3(300.0f,400.0f,10.0f),
													"Exit/n",
													"OCR Extended/n",
													12,
													400,
													FF_ROMAN,
													0x00880000,
													TRUE,
													0x00ff0000,
													255,
													255,
													255);

						g_AppState.ChangeAppState (APP_MENUACTIVE);
						break;
					}

				case APP_MENUACTIVE:
					{
						//query buttons
						//send click events
						//g_pMainMenu->Render(g_pd3dDevice);
						break;
					}

				case APP_TERMINATE:
					{
						MainMenuRelease(g_pMainMenu);
						PostQuitMessage(0);
						notDone = FALSE;
						break;
					}
			}

			//temporary
			g_pd3dDevice->Clear( 0, NULL, D3DCLEAR_TARGET, D3DCOLOR_XRGB(100,100,0), 1.0f, 0L );
			g_pd3dDevice->BeginScene();

			SetupMatrices();

			g_pd3dDevice->EndScene();

			//present the frame here
			g_pd3dDevice->Present(NULL, NULL, NULL, NULL);
		}
	}

	Cleanup();
	//exit message

	return (msg.wParam);

}

LRESULT CALLBACK WindowProc(HWND hWnd, unsigned uMsg, WPARAM wParam, LPARAM lParam)
{
	g_hThisWnd = hWnd;
	switch (uMsg)
	{

        case WM_PAINT:
            ValidateRect( hWnd, NULL );
            break;

		case WM_QUIT:
			DestroyWindow(g_hThisWnd);
			break;

		case WM_CLOSE:
			g_AppState.ChangeAppState (APP_TERMINATE);
			break;

		case WM_KEYDOWN:
			switch (wParam)
			{
				case VK_ESCAPE:
					g_AppState.ChangeAppState (APP_TERMINATE);
					break;
				default:
					break;
			}
			break;

		case WM_LBUTTONUP:
			{
				int xPos = LOWORD(lParam);  // horizontal position of cursor 
				int yPos = HIWORD(lParam);  // vertical position of cursor 
				g_pMainMenu->Click (xPos, yPos);
			}
			break;


		default:
			return DefWindowProc(hWnd, uMsg, wParam, lParam);
	}
	
	return 0L;
}


BOOL Initialize(HINSTANCE hInstance, int nCmdShow, HWND hWnd1)
{
	HRESULT			hRet;

	hRet = CreateMyAppWindow(hInstance, hWnd1);
	if (hRet != S_OK)
	{
		ErrStr = Err_Create_Win;
		return FALSE;
	}

	hRet = InitD3D8(g_hThisWnd);
	if (hRet != S_OK)
	{
		ErrStr = Err_Direct3DCreate;
		return FALSE;
	}
	else
	{
		ShowWindow(g_hThisWnd,nCmdShow);
		UpdateWindow(g_hThisWnd);
	}


	hRet = InitDI8(hInstance, g_hThisWnd);
	if (hRet != S_OK)
	{
		return FALSE;
	}

	hRet = InitDS8(g_hThisWnd);
	if (hRet != S_OK)
	{
		return FALSE;
	}

	hRet = SetupTimer();
	if (hRet != S_OK)
	{
		return FALSE;
	}


	//initialization complete

	g_bInit = TRUE;

	return TRUE;

}

void Cleanup(void)
{
//release interfaces

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

	//display error if thrown
	if(ErrStr)
	{
		ofstream ofile( "ERRORLOG", ios::out );
		ofile.open( "FILE1", ios::in );
		ofile.write(ErrStr, sizeof(ErrStr) );
		ofile.close(); // FILE1 closed

		MessageBox(g_hThisWnd, ErrStr, NULL, MB_OK);
	}
}




HRESULT CreateMyAppWindow(HINSTANCE hInstance, HWND hWnd2)
{
	//hWin class
 	WNDCLASS		wc;
	//properties
	wc.style = CS_HREDRAW | CS_VREDRAW;
	wc.lpfnWndProc = (WNDPROC) WindowProc; 
	wc.cbClsExtra = 0;
	wc.cbWndExtra = sizeof(DWORD);
	wc.hInstance = hInstance;
	wc.hIcon = LoadIcon(hInstance,MAKEINTRESOURCE(ID1));
	wc.hCursor = LoadCursor(NULL, IDC_ARROW);
	wc.hbrBackground = (HBRUSH) GetStockObject(BLACK_BRUSH);
	wc.lpszMenuName = NULL;
	wc.lpszClassName = szClass;


	if (!RegisterClass(&wc))
	{	
		ErrStr = Err_Reg_Class;
		return S_FALSE;
	}


	switch ((int)g_AppState.QueryAppWindowState ())
	{
	case APP_WINDOWED:
		{
			//dimensions of window

			int ScreenWidth = 800;
			int ScreenHeight = 600;

			//window creation and display

			hWnd2 = CreateWindow(szClass,
								NULL,
								WS_OVERLAPPEDWINDOW,//WS_VISIBLE|WS_POPUP
								0,
								0,
								ScreenWidth,
								ScreenHeight,
								NULL,
								NULL,
								hInstance,
								NULL);
			break;
		}
	case APP_FULLSCREEN:
		{
			//dimensions of window

			int ScreenWidth = 800;
			int ScreenHeight = 600;
			
			//window creation and display

			hWnd2 = CreateWindow(szClass,
								NULL,
								WS_VISIBLE|WS_POPUP|WS_EX_TOPMOST,//||WS_OVERLAPPEDWINDOW
								0,
								0,
								ScreenWidth,
								ScreenHeight,
								NULL,
								NULL,
								hInstance,
								NULL);
			break;
		}
	default:
		{
		}
	}


	if (!hWnd2)
	{
		return S_FALSE;
	}

	return S_OK;

}


HRESULT InitD3D8(HWND hWnd3)
{
    // Create the D3D object, which is needed to create the D3DDevice.
    if( NULL == ( g_pd3d = Direct3DCreate8( D3D_SDK_VERSION ) ) )
        return E_FAIL;

    D3DPRESENT_PARAMETERS d3dpp; 
    ZeroMemory( &d3dpp, sizeof(d3dpp) );
	D3DDISPLAYMODE d3ddm;

	switch ((int)g_AppState.QueryAppWindowState ())
	{
	case APP_WINDOWED:
		{
			// Get the current desktop display mode
			
			if( FAILED( g_pd3d->GetAdapterDisplayMode( D3DADAPTER_DEFAULT, &d3ddm ) ) )
				return E_FAIL;

			d3dpp.hDeviceWindow = g_hThisWnd;
			d3dpp.Windowed = TRUE;
			d3dpp.BackBufferWidth = 800;
			d3dpp.BackBufferHeight = 600;
			d3dpp.BackBufferFormat = d3ddm.Format;
			break;
		}
	case APP_FULLSCREEN:
		{

			// Get the current desktop display mode
			
			if( FAILED( g_pd3d->GetAdapterDisplayMode( D3DADAPTER_DEFAULT, &d3ddm ) ) )
				return E_FAIL;

			d3dpp.hDeviceWindow = g_hThisWnd;
			d3dpp.Windowed = FALSE;
			d3dpp.BackBufferWidth = 800;
			d3dpp.BackBufferHeight = 600;
			d3dpp.BackBufferFormat = d3ddm.Format;
			d3dpp.FullScreen_RefreshRateInHz = d3ddm.RefreshRate;
			d3dpp.FullScreen_PresentationInterval = D3DPRESENT_INTERVAL_ONE;	
			break;
		}
	default:
		{
		}
	}

	d3dpp.BackBufferCount = 1;

    d3dpp.SwapEffect = D3DSWAPEFFECT_COPY_VSYNC;

    if( FAILED( g_pd3d->CreateDevice( D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, hWnd3,
                                      D3DCREATE_SOFTWARE_VERTEXPROCESSING,
                                      &d3dpp, &g_pd3dDevice ) ) )
    {
        return E_FAIL;
    }

	//link up the camera
	g_Camera.Initialize (g_pd3dDevice);
    return S_OK;
}


HRESULT InitDI8(HINSTANCE hInstance, HWND hWnd4)
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
	hRet = g_pdiKeyboard->SetCooperativeLevel(hWnd4, DISCL_FOREGROUND|DISCL_EXCLUSIVE);
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
	hRet = g_pdiMouse->SetCooperativeLevel(hWnd4, DISCL_FOREGROUND|DISCL_EXCLUSIVE);
	if (hRet != DI_OK)
	{
		ErrStr = Err_DeviceCoop;
		return S_FALSE;
	}

	//acquire mouse

	return S_OK;
}

HRESULT InitDS8(HWND hWnd5)
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

	hRet = g_pds8->SetCooperativeLevel (hWnd5,DSSCL_NORMAL);
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

HRESULT SetupMatrices(void)
{
    // For our world matrix, we will just leave it as the identity
    D3DXMatrixIdentity( &g_matWorld );
    g_pd3dDevice->SetTransform( D3DTS_WORLD, &g_matWorld );

	g_Camera.DefaultSetup();

	return S_OK;
}