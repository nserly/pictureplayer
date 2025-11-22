#pragma once
#include <windows.h>
#include <iostream>
#include <tchar.h>
#include <vector>
#include <shellapi.h>
#include <shlwapi.h>
#include <string>
#include <io.h>
#include <fstream>
#include <fcntl.h>

#include "AppUtils.h"
#include "FileUtils.h"
#include "ConsoleUtils.h"
#include "JOptionPane.h"
#include "PropertiesReader.h"
#include "TcpClient.h"

#pragma comment(lib, "shlwapi.lib")


int WINAPI WinMain(_In_ HINSTANCE hInstance,_In_opt_ HINSTANCE hPrevInstance,_In_ LPSTR lpCmdLine,_In_ int nCmdShow);