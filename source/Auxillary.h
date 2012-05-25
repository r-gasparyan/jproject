#pragma once
#include <string>
#include <time.h>

// translates string to uppercase
std::string toUpper				( const std::string& source );

// splits a string containing delimeters into a vector of tokens
void Split						(const std::string& str, std::vector<std::string>& tokens, const std::string& delimiters);

// all http responses returns in one line
std::string GetLastLine			( const std::string& str );

// replaces string "rep_what" by string "rep_to" in the source string "src"
std::string Replace				( const std::string& src, const std::string& rep_what, const std::string& rep_to = "" );

// returns string containing time information in user format
std::string DateTime			( const std::string& format );

// returns encoded string (all escaped symbols properly replaced)
std::string urlencode			( const std::string &s );

// check if the file exists in the current  directory
bool FileExists					( const std::string& filename );

// remove spaces in the source string
std::string RemoveSpaces( const std::string& src );

std::string fromInt( int value );

std::string fromDouble( double value );

std::vector<std::string> Split( const std::string& str, const std::string& delimiters );

bool isItemValidForMask( const std::string& item, const std::string& maskValue );

bool checkMask( const std::string& value, const std::string mask );

// write line to log file
void Log( const char* line);