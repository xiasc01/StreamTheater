/************************************************************************************

Filename    :   CinemaStrings.cpp
Content     :	Text strings used by app.  Located in one place to make translation easier.
Created     :	9/30/2014
Authors     :   Jim Dosé

Copyright   :   Copyright 2014 Oculus VR, LLC. All Rights reserved.

This source code is licensed under the BSD-style license found in the
LICENSE file in the Cinema/ directory. An additional grant 
of patent rights can be found in the PATENTS file in the same directory.

*************************************************************************************/

#include "CinemaStrings.h"
#include "VrLocale.h"
#include "CinemaApp.h"
#include "BitmapFont.h"

namespace VRMatterStreamTheater
{
String CinemaStrings::LoadingMenu_Title;

String CinemaStrings::Category_Trailers;
String CinemaStrings::Category_MyVideos;
String CinemaStrings::Category_LimeLight;
String CinemaStrings::Category_RemoteDesktop;
String CinemaStrings::Category_VNC;

String CinemaStrings::MovieSelection_Resume;
String CinemaStrings::MovieSelection_Next;

String CinemaStrings::ResumeMenu_Title;
String CinemaStrings::ResumeMenu_Resume;
String CinemaStrings::ResumeMenu_Restart;

String CinemaStrings::TheaterSelection_Title;

String CinemaStrings::Error_NoVideosOnPhone;
String CinemaStrings::Error_NoVideosInLimeLight;
String CinemaStrings::Error_NoApps;
String CinemaStrings::Error_UnableToPlayMovie;

String CinemaStrings::MoviePlayer_Reorient;

String CinemaStrings::ButtonText_ButtonSaveApp;
String CinemaStrings::ButtonText_ButtonSaveDefault;
String CinemaStrings::ButtonText_ButtonResetSettings;
String CinemaStrings::ButtonText_ButtonSaveSettings1;
String CinemaStrings::ButtonText_ButtonSaveSettings2;
String CinemaStrings::ButtonText_ButtonSaveSettings3;
String CinemaStrings::ButtonText_ButtonLoadSettings1;
String CinemaStrings::ButtonText_ButtonLoadSettings2;
String CinemaStrings::ButtonText_ButtonLoadSettings3;
String CinemaStrings::ButtonText_ButtonMapKeyboard;
String CinemaStrings::ButtonText_ButtonSpeed;
String CinemaStrings::ButtonText_ButtonComfortMode;
String CinemaStrings::ButtonText_Button1080;
String CinemaStrings::ButtonText_Button720;
String CinemaStrings::ButtonText_Button60FPS;
String CinemaStrings::ButtonText_Button30FPS;
String CinemaStrings::ButtonText_ButtonHostAudio;
String CinemaStrings::ButtonText_ButtonApply;
String CinemaStrings::ButtonText_ButtonBitrate;
String CinemaStrings::ButtonText_ButtonDistance;
String CinemaStrings::ButtonText_ButtonSize;
String CinemaStrings::ButtonText_ButtonSBSOff;
String CinemaStrings::ButtonText_ButtonSBSRift;
String CinemaStrings::ButtonText_ButtonSBSCrop;
String CinemaStrings::ButtonText_ButtonSBSScale;
String CinemaStrings::ButtonText_ButtonChangeSeat;
String CinemaStrings::ButtonText_ButtonGaze;
String CinemaStrings::ButtonText_ButtonTrackpad;
String CinemaStrings::ButtonText_LabelTrackpadScale;
String CinemaStrings::ButtonText_LabelGamepadScale;
String CinemaStrings::ButtonText_ButtonOff;
String CinemaStrings::ButtonText_ButtonGamepad;
String CinemaStrings::ButtonText_LabelGazeScale;
String CinemaStrings::ButtonText_LabelLatency;
String CinemaStrings::ButtonText_LabelVRXScale;
String CinemaStrings::ButtonText_LabelVRYScale;
String CinemaStrings::ButtonText_CloseApp;
String CinemaStrings::ButtonText_Settings;

String CinemaStrings::HelpText;

String CinemaStrings::Error_UnknownHost;
String CinemaStrings::Error_AddPCFailed;

void CinemaStrings::OneTimeInit( CinemaApp &cinema )
{
	LOG( "CinemaStrings::OneTimeInit" );

	App *app = cinema.app;

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/LoadingMenu_Title", 		"@string/LoadingMenu_Title", 		LoadingMenu_Title );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Category_Trailers", 		"@string/Category_Trailers", 		Category_Trailers );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Category_MyVideos", 		"@string/Category_MyVideos", 		Category_MyVideos );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Category_LimeLight", 		"@string/Category_LimeLight", 		Category_LimeLight );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Category_RemoteDesktop", 	"@string/Category_RemoteDesktop", 	Category_RemoteDesktop );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Category_VNC", 			"@string/Category_VNC", 			Category_VNC );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/MovieSelection_Resume",	"@string/MovieSelection_Resume",	MovieSelection_Resume );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/MovieSelection_Next", 		"@string/MovieSelection_Next", 		MovieSelection_Next );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ResumeMenu_Title", 		"@string/ResumeMenu_Title", 		ResumeMenu_Title );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ResumeMenu_Resume", 		"@string/ResumeMenu_Resume", 		ResumeMenu_Resume );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ResumeMenu_Restart", 		"@string/ResumeMenu_Restart", 		ResumeMenu_Restart );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/TheaterSelection_Title", 	"@string/TheaterSelection_Title", 	TheaterSelection_Title );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Error_NoVideosOnPhone", 	"@string/Error_NoVideosOnPhone", 	Error_NoVideosOnPhone );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Error_NoVideosInLimeLight", "@string/Error_NoVideosInLimeLight", Error_NoVideosInLimeLight );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Error_NoApps",		 		 "@string/Error_NoApps",			  Error_NoApps );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/Error_UnableToPlayMovie", 	"@string/Error_UnableToPlayMovie",	Error_UnableToPlayMovie );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/MoviePlayer_Reorient", 	"@string/MoviePlayer_Reorient", 	MoviePlayer_Reorient );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSaveApp", 		"@string/ButtonText_ButtonSaveApp", 		ButtonText_ButtonSaveApp );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSaveDefault", 	"@string/ButtonText_ButtonSaveDefault", 	ButtonText_ButtonSaveDefault );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonResetSettings",	"@string/ButtonText_ButtonResetSettings",	ButtonText_ButtonResetSettings );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSaveSettings1",	"@string/ButtonText_ButtonSaveSettings1", 	ButtonText_ButtonSaveSettings1 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSaveSettings2",	"@string/ButtonText_ButtonSaveSettings2", 	ButtonText_ButtonSaveSettings2 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSaveSettings3",	"@string/ButtonText_ButtonSaveSettings3", 	ButtonText_ButtonSaveSettings3 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonLoadSettings1",	"@string/ButtonText_ButtonLoadSettings1", 	ButtonText_ButtonLoadSettings1 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonLoadSettings2",	"@string/ButtonText_ButtonLoadSettings2", 	ButtonText_ButtonLoadSettings2 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonLoadSettings3",	"@string/ButtonText_ButtonLoadSettings3", 	ButtonText_ButtonLoadSettings3 );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonMapKeyboard", "@string/ButtonText_ButtonMapKeyboard", 	ButtonText_ButtonMapKeyboard );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSpeed", 		"@string/ButtonText_ButtonSpeed", 			ButtonText_ButtonSpeed );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonComfortMode", "@string/ButtonText_ButtonComfortMode", 	ButtonText_ButtonComfortMode );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonHostAudio", 	"@string/ButtonText_ButtonHostAudio", 		ButtonText_ButtonHostAudio );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonApply", 		"@string/ButtonText_ButtonApply", 			ButtonText_ButtonApply );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonBitrate", 	"@string/ButtonText_ButtonBitrate", 		ButtonText_ButtonBitrate );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_Button1080", 		"@string/ButtonText_Button1080", 			ButtonText_Button1080 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_Button720",	 		"@string/ButtonText_Button720",				ButtonText_Button720 );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_Button60FPS", 		"@string/ButtonText_Button60FPS", 			ButtonText_Button60FPS );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_Button30FPS", 		"@string/ButtonText_Button30FPS", 			ButtonText_Button30FPS );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonDistance", 	"@string/ButtonText_ButtonDistance", 		ButtonText_ButtonDistance );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSize", 		"@string/ButtonText_ButtonSize", 			ButtonText_ButtonSize );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSBSOff", 		"@string/ButtonText_ButtonSBSOff", 			ButtonText_ButtonSBSOff );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSBSRift",		"@string/ButtonText_ButtonSBSRift",			ButtonText_ButtonSBSRift );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSBSCrop",		"@string/ButtonText_ButtonSBSCrop",			ButtonText_ButtonSBSCrop );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonSBSScale",	"@string/ButtonText_ButtonSBSScale",		ButtonText_ButtonSBSScale );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonChangeSeat", 	"@string/ButtonText_ButtonChangeSeat", 		ButtonText_ButtonChangeSeat );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonGaze",	 	"@string/ButtonText_ButtonGaze", 			ButtonText_ButtonGaze );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonTrackpad", 	"@string/ButtonText_ButtonTrackpad", 		ButtonText_ButtonTrackpad );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonOff", 		"@string/ButtonText_ButtonOff", 			ButtonText_ButtonOff );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_ButtonGamepad",		"@string/ButtonText_ButtonGamepad",			ButtonText_ButtonGamepad );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_LabelGazeScale",	"@string/ButtonText_LabelGazeScale", 		ButtonText_LabelGazeScale );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_LabelTrackpadScale","@string/ButtonText_LabelTrackpadScale", 	ButtonText_LabelTrackpadScale );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_LabelGamepadScale",	"@string/ButtonText_LabelGamepadScale", 	ButtonText_LabelGamepadScale );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_LabelLatency",		"@string/ButtonText_LabelLatency", 			ButtonText_LabelLatency );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_LabelVRXScale",		"@string/ButtonText_LabelVRXScale", 		ButtonText_LabelVRXScale );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_LabelVRYScale",		"@string/ButtonText_LabelVRYScale", 		ButtonText_LabelVRYScale );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_CloseApp",			"@string/ButtonText_CloseApp", 				ButtonText_CloseApp );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/ButtonText_Settings",			"@string/ButtonText_Settings", 				ButtonText_Settings );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/HelpText",						"@string/HelpText", 						HelpText );

	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/addpc_unknown_host",			"@string/addpc_unknown_host", 				Error_UnknownHost );
	VrLocale::GetString( app->GetVrJni(), app->GetJavaObject(), "@string/addpc_fail",					"@string/addpc_fail", 						Error_AddPCFailed );
}

} // namespace VRMatterStreamTheater
