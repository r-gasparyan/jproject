std::string toUpper( const std::string& source )
{
	std::string result;

	int size = source.length();
	for (int i = 0; i < size; ++i)
	{
		char ch = source[i];
		char up = toupper(ch);
		result.push_back(up);
	}

	return result;
}

void Split(const std::string& str, std::vector<std::string>& tokens, const std::string& delimiters)
{
	std::string::size_type lastPos = str.find_first_not_of(delimiters, 0);
	std::string::size_type pos = str.find_first_of(delimiters, lastPos);
	while (std::string::npos != pos || std::string::npos != lastPos)
    {
        tokens.push_back(str.substr(lastPos, pos - lastPos));
        lastPos = str.find_first_not_of(delimiters, pos);
        pos = str.find_first_of(delimiters, lastPos);
    }
}

// all http responses returns in one line
std::string GetLastLine( const std::string& str )
{
	char ch = 10;
	std::string::size_type pos = str.find_last_of(ch) + 1;
	return str.substr( pos );
}

std::string Replace( const std::string& src, const std::string& rep_what, const std::string& rep_to )
{
	std::string::size_type start = src.find(rep_what);
	if (start != std::string::npos)
		return src.substr(0, start) + rep_to + src.substr(start + rep_what.length());	
	else
		return src;
}

std::string DateTime(const std::string& format)
{
    time_t rawtime = time( &rawtime );
    struct tm* timeinfo = localtime( &rawtime );

    char buffer [128];
    strftime (buffer,128,format.c_str(),timeinfo);
    
    return std::string(buffer);
}

std::string urlencode(const std::string &s)
{
    //RFC 3986 section 2.3 Unreserved Characters (January 2005)
    const std::string unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~";

    std::string escaped="";
    for(size_t i=0; i<s.length(); i++)
    {
        if (unreserved.find_first_of(s[i]) != std::string::npos)
        {
            escaped.push_back(s[i]);
        }
        else
        {
            escaped.append("%");
            char buf[3];
            sprintf(buf, "%.2X", s[i]);
            escaped.append(buf);
        }
    }
    return escaped;
}

bool FileExists(const std::string& filename)
{
	return GetFileAttributes(filename.c_str()) != 0xFFFFFFFF;
}

std::string RemoveSpaces( const std::string& src )
{
	std::string result;
	for(int i = 0; i < src.size(); ++i)
	{
		char ch = src[i];
		
		if ( ch != ' ' )
		{		
			result.push_back( ch );
		}		
	}
	return result;
}

std::string fromInt( int value )
{
	char buf[128];
	sprintf_s(buf, 128, "%d", value);
	return std::string(buf);
}

std::string fromDouble( double value )
{
	char buf[128];
	sprintf_s(buf, 128, "%0.2f", value);
	return std::string(buf);
}

std::vector<std::string> Split( const std::string& str, const std::string& delimiters )
{
        std::vector<std::string> tokens;

        std::string::size_type lastPos = str.find_first_not_of(delimiters, 0);
        std::string::size_type pos = str.find_first_of(delimiters, lastPos);
        while (std::string::npos != pos || std::string::npos != lastPos)
    {
        tokens.push_back(str.substr(lastPos, pos - lastPos));
        lastPos = str.find_first_not_of(delimiters, pos);
        pos = str.find_first_of(delimiters, lastPos);
    }
        return tokens;
}

bool isItemValidForMask( const std::string& item, const std::string& maskValue )
{
        bool isValid = true;

        bool bFirst = false;
        bool bLast = false;

        if(maskValue.at(0) == '*') bFirst = true;
        if(maskValue.at(maskValue.length()-1) == '*') bLast = true;

        std::vector<std::string> maskItems = Split(maskValue, "*");

        int oldPos = 0;

        for(int i = 0; i < maskItems.size(); i++)
        {
                int l = item.length() - 1 - maskItems[i].length() - 1;
                int pos = item.find(maskItems[i]);
                if(pos == std::string::npos ||
                        pos < oldPos ||
                        (i == 0 && !bFirst && pos != 0) ||
                        (i == maskItems.size() - 1 && !bLast && pos != (item.length() - maskItems[i].length()) ) )
                {
                        isValid = false;
                        break;
                }
                oldPos = pos;
        }
        return isValid;
}

bool checkMask( const std::string& value, const std::string mask )
{
    std::vector<std::string> masks = Split(mask, ",");

    bool bValid = false;
    for(unsigned int i = 0; i < masks.size();i++)
    {
        if(masks[i].at(0) == '!')
        {
            std::string mask = masks[i].substr(1);
            if( isItemValidForMask(value, mask) )
            {
                bValid = false;
                break;
            }
        }
                else
        {
            bValid = bValid || isItemValidForMask(value, masks[i]);
        }
    }
    return bValid;
}

void Log( const char* line )
{
#ifdef _DEBUG
	std::ofstream out_stream;
	out_stream.open( "CBRSiteLog.txt", std::ofstream::app );
	out_stream << DateTime("[%Y-%m-%d %H:%M]") << line << std::endl; 
	out_stream.close();
#endif
}