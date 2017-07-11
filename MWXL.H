#ifndef _MWXL_H_
#define _MWXL_H_

//  Master World X-File List
class MWXL
{
private:
WorldXFileList m_MyWXL;
WorldXFileList::const_iterator m_ci;
WorldXFileList::iterator m_nci;
float m_ListVersion;

public:
	MWXL();
	~MWXL();
	HRESULT Add_X_File(XFILEENTRY NewEntry, bool IncrementVersion);
	HRESULT Add_X_Files_By_List(list<LPSTR> NewFiles, GEOMETRYTYPE GeometryType, bool IncrementVersion);
	XFILEENTRY Get_Complete_X_File(unsigned int Index);
	float Get_Version();
	bool Is_Updated(float UserVersion);
	HRESULT Remove_X_File(unsigned int Index, bool IncrementVersion);
	HRESULT Replace_X_File(unsigned int Index, XFILEENTRY NewEntry, bool IncrementVersion);
	list<XFILEENTRY> Request_X_File_List();
	HRESULT Save_X_File_List(char FileName[XFileList_FileNameLength]);
	int Sizeof_List(void);
};


MWXL::MWXL()
{
	m_ci = m_MyWXL.begin();
	m_nci = m_MyWXL.begin();
	m_ListVersion = 0.0f;
}


MWXL::~MWXL ()
{
	m_MyWXL.clear();
}


HRESULT MWXL::Add_X_File(XFILEENTRY NewEntry, bool IncrementVersion)
{
		//create new blank entry
		LPXFILEENTRY NewXFileEntry = new XFILEENTRY;

		//update list version?
		if (IncrementVersion)
		{
			m_ListVersion += 0.00001f;
		}

		//fill it with values
		(*NewXFileEntry).CreationDate = NewEntry.CreationDate;
		(*NewXFileEntry).Filename = NewEntry.Filename;
		(*NewXFileEntry).FileSize = NewEntry.FileSize;
		(*NewXFileEntry).GeoType = NewEntry.GeoType;
		(*NewXFileEntry).VersionAdded = m_ListVersion;

		//add it to the master list
		m_MyWXL.push_back(*NewXFileEntry);

		return S_OK;
}

HRESULT MWXL::Add_X_Files_By_List(list<LPSTR> NewFiles, GEOMETRYTYPE GeometryType, bool IncrementVersion)
{
	//update list version?
	if (IncrementVersion)
	{
		m_ListVersion += 0.00001f;
	}

	list<LPSTR>::const_iterator ci;
	ci = NewFiles.begin();

	//loop and parse tokens out of buffer and place into new buffer
	//start a loop with the count of entries
	for(int i = 0; i < NewFiles.size(); i++, ci++)
	{
		//create new blank entry
		LPXFILEENTRY NewXFileEntry = new XFILEENTRY;

		//fill it with values
		(*NewXFileEntry).Filename = *ci;
		(*NewXFileEntry).GeoType = GeometryType;
		(*NewXFileEntry).VersionAdded = m_ListVersion;

		//open said file to get file attributes
		HANDLE hThisFile;
		hThisFile = CreateFile((*NewXFileEntry).Filename,GENERIC_READ, FILE_SHARE_READ, 
			NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);


		//get filesize for input
		DWORD dwFileSize;
		dwFileSize = GetFileSize(hThisFile, &dwFileSize);

		//store the value 
		(*NewXFileEntry).FileSize = dwFileSize;

		//close the file handle
		CloseHandle(hThisFile);

		//add it to the master list
		m_MyWXL.push_back(*NewXFileEntry);

		//loop to next entry
	}

		return S_OK;
}


XFILEENTRY MWXL::Get_Complete_X_File(unsigned int Index)
{
	if (Index > (m_MyWXL.size() - 1))
	{
		//index out of bounds
		m_ci = m_MyWXL.end();
		return *m_ci;
	}
	else
	{
		// get X File Name
		m_ci = m_MyWXL.begin();

		//iterate to the element
		if (Index == 0)
		{
			return *m_ci;
		}
		else
		{
			int i = 0;
	
			do
			{
				++i;
				++m_ci;
			}
			while(i <= (Index - 1));
	
			return *m_ci;
		}
	}
}


float MWXL::Get_Version()
{
	return m_ListVersion;
}


bool MWXL::Is_Updated(float UserVersion)
{
	if (UserVersion != m_ListVersion)
	{
		return true;
	}
	else
	{
		return false;
	}
}


HRESULT MWXL::Remove_X_File (unsigned int Index, bool IncrementVersion)
{

	if (Index > m_MyWXL.size())
	{
		//index out of bounds
		return S_FALSE;
	}
	else
	{
		// remove X File Name
		m_nci = m_MyWXL.begin();

		//iterate to the element
		if (Index == 0)
		{
			m_MyWXL.erase(m_nci);
		}
		else
		{
			int i = 0;
	
			do
			{
				++i;
				++m_nci;
			}
			while(i <= (Index - 1));

			m_MyWXL.erase(m_nci);
		}

		//update list version?
		if (IncrementVersion)
		{
			m_ListVersion += 0.00001f;
		}

		return S_OK;
	}
}


HRESULT MWXL::Replace_X_File(unsigned int Index, XFILEENTRY NewEntry, bool IncrementVersion)
{
	if (Index > m_MyWXL.size())
	{
		//index out of bounds
		return S_FALSE;
	}
	else
	{
		// remove X File 
		m_nci = m_MyWXL.begin();

		//iterate to the element
		if (Index == 0)
		{
			m_MyWXL.erase(m_nci);
		}
		else
		{
			int i = 0;
	
			do
			{
				++i;
				++m_nci;
			}
			while(i <= (Index - 1));

			m_MyWXL.erase(m_nci);
		}

		//update list version?
		if (IncrementVersion)
		{
			m_ListVersion += 0.00001f;
		}

		//writein list version to new entry
		NewEntry.VersionAdded = m_ListVersion;

		//replace with new entry
		m_MyWXL.insert(m_nci, NewEntry);




		return S_OK;
	}
}


list<XFILEENTRY> MWXL::Request_X_File_List()
{
	return m_MyWXL;
}


HRESULT MWXL::Save_X_File_List(char FileName[XFileList_FileNameLength])
{
	//open file for writting
	::ofstream fout(FileName);

	//output the list size
	int listsize = m_MyWXL.size();
	fout << listsize << ";;" << "\n";

	//loop through list and add names to the file
	m_ci = m_MyWXL.begin ();

	//iterate through the elements
	int i = 0;
	do
	{
		fout << (*m_ci).GeoType << ";" << "\n";
		fout << (*m_ci).Filename <<  ";" << "\n";
		fout << (*m_ci).CreationDate <<  ";" << "\n";
		fout << (*m_ci).FileSize <<  ";" << "\n";
		fout << (*m_ci).VersionAdded <<  ";;" << "\n";
		++i;
		++m_ci;
	}
	while(i < listsize);

	//output list version
	fout << m_ListVersion << "\n";

	//close the file
	fout.close();

	return S_OK;
}


int MWXL::Sizeof_List(void)
{
	return m_MyWXL.size();
}

#endif //_MWXL_H_