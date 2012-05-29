bool checkLicense(HMODULE handle)
{
	if (handle)
	{
		// all ok
		return true;
	}
	else
	{
		// something wrong
		return false;
	}
}