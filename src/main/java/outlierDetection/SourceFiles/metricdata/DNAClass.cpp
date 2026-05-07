/**@file DNAClass.h
* @brief This is a Index object of DNA.
* @author Yaoda Liu(2011150337@email.szu.edu.cn)
* @date 2013/3/18
*
* This class define DNA index object. It load data from a format file.
*/



#include "../../HeaderFiles/metricdata/DNAClass.h"
#include <sstream>

/** A  static data set stored DNA symbols.  */
CDNASymbol CDNAClass::DNASymbols[DNASYMBOLNUMBER] = 
{
    {0, 'A', "Adenine"}, {1, 'C', "Cytosine"}, {2, 'G', "Guanie"}, {3, 'T', "Thymine"},
    {4, 'R', "Purine"}, {5, 'Y', "Pyrimidine"}, {6, 'M', "C or A"}, {7, 'K', "T, U, or G"},
    {8, 'W', "T, U or A"}, {9, 'S', "C or G"}, {10, 'G', "not A"}, {11, 'D', "not C"},
    {12, 'H', "not G"}, {13, 'V', "not T, U"}, {14, 'N', "Any base"}
};

/**
* @brief No parameters constructor. Doing nothing.
*/
CDNAClass::CDNAClass()
{
}



/**
* @brief This constructor initial DNA sequence.
* @param sid          Identity string of this DNA sequence.
* @param sequence     Sequence of this DNA.
*
* @note
* The first string parameter will be assigned to _sequenceid.
* The second string parameter will be assigned to _sequence.
* Property _size will be assigned by _sequence's length.
* Each characters in sequence will translate into symbol id and stored in _symbolIDs.
*/

CDNAClass::CDNAClass(string sid, string sequence, bool _isNormal)
{
    int i;
    int temp;
	this->_sequenceid = sid;
	this->isNormal = _isNormal;
	this->_sequence = sequence;
	this->_size = this->_sequence.size();
	for(i=0;i<this->_size;i++)
	{
		temp = CDNAClass::getSymbolID(this->_sequence.at(i));
		this->_symbolIDs.push_back(temp);
	}
}



/** 
* A destructor, do nothing. 
*/
CDNAClass::~CDNAClass()
{
}

/**
* @return Return the size of DNA sequence.
*/
int CDNAClass::getSize()const
{
    return this->_size;
}

bool CDNAClass::getState()
{
	return isNormal;
}

/**
* @brief  Get the sysbolID list.
* @return Return a integer vector contain symbols of DNA sequence.
*/
vector<int> CDNAClass::getSymbolIDs()const
{
    return this->_symbolIDs;
}

/**
* @brief A static function to return symbol's ID.
*        Get the symbol's id according to input symbol character.
*        This function required a input param which is existed in DNASymbol data set.
* @param symbol    A symbol waiting to get its ID.
* @return  Return a int ID which is stand for input char.
*/
int CDNAClass::getSymbolID(char symbol)
{
    int i;
    for(i=0;i<DNASYMBOLNUMBER;i++)
        if(symbol == DNASymbols[i].abbr)
            return DNASymbols[i].sid;
    return -1;
}

/**
* @brief    A static function to load data from a file.
* This function will get data from a format file, which contain some DNA informations,
*  and then save as a CDNA type and store in a vector.
*
* @note
* Firstly, load each DNA sequence according to the characters from the file,
*  the char '>' in the format files, stand for the beginning of DNA,
*  if the length of total characters more than maxSize the function will
*  stop loadding.
* Then, split DNA sequenct into many pieces, each piece's length is fragmentLength.
* Finally, save all pieces in a vector, and return this vector.
* A object defined shared_ptr<T>(a kind of smart pointer, existed in <memory>) will count
*  how much pointer point to it, and this counting called reference counting.
*  When reference counting reduces to 0, this object will destory automatically.
* By using shared_ptr<T>, we can ensure no memory leak.
* 
* @param filename           The file path of input data.
* @param maxSize            The max size of loaded data.
* @param fragmentLength     The length of each DNA fragment.
* @return return a vector stored DNA fragments.
*/

vector<shared_ptr<CMetricData> >* CDNAClass::loadData(string filename, int maxSize, int &outlierNum)
{
    vector<shared_ptr<CMetricData> > *data = new vector<shared_ptr<CMetricData> >;
    vector<shared_ptr<CDNAClass> > dnas;
	ifstream infile(filename.c_str(), ios::in);
    if(!infile.is_open())
	{
        cerr << "Stop! Cannot open the file." << endl;
        return data;
    }

    string ident = "";
    int i;
    int counter = 0;
    int sequenceLengthCounter = 0;
    string currentSequence;
	bool _isNormal = true;
    string line;
    //char buffer[65];
	/**actual outlier number*/
	int aon = 0;
	int dim = 0;
	int size = 0;
	int num = 0;
	infile>>dim>>size;
	size = size>maxSize ? maxSize:size;
	getline(infile, line);
	//cout<<dim<<"  "<<size<<endl;
    //while(!infile.eof() && counter < size && sequenceLengthCounter < maxSize)
	while(counter < size)
    {
        getline(infile, line);
		stringstream newStr(line);
		newStr>>currentSequence>>_isNormal;
		//cout<<currentSequence<<"  "<<_isNormal<<endl;
		shared_ptr<CDNAClass> temp(new CDNAClass(ident, currentSequence, _isNormal));
        data->push_back(temp);
		counter++;
		if(!_isNormal)
		{
			aon++;
		}
	}
	/*cout<<data->size()<<endl;
	for(int i=0; i<10; i++)
	{
		cout<<((CDNAClass*)(*data).at(i).get())->getSize()<<endl;
	}
	system("pause");*/
	outlierNum = aon;
    return data;
}

/*
int CDNAClass::writeExternal(ofstream &out)
{
    int size = 0;
    int temp;
    temp = _sequenceid.size() + 1;
    out.write((char*) (&(temp)), sizeof(int));
    size += sizeof(int);
    out.write((char*) (_sequenceid.c_str()), (temp) * sizeof(char));
    size += (_sequenceid.size()+1) * sizeof(char);
    temp = _sequence.size() + 1;
    out.write((char*) (&(temp)), sizeof(int));
    size += sizeof(int);
    out.write((char*) (_sequence.c_str()), (temp) * sizeof(char));
    size += sizeof(char) * (_size+1);
    temp = _symbolIDs.size();
    out.write((char*) (&(temp)), sizeof(int));
    size += sizeof(int);
    for (int i=0; i<_symbolIDs.size(); ++i){
        out.write((char*) ((&_symbolIDs.at(i))), sizeof(int));
        size += sizeof(int);
    }
    return size;
}

int CDNAClass::readExternal(ifstream &in)
{
    _sequence.clear();
	isNormal = true;
    _symbolIDs.clear();
    int size = 0;
    int tempSize;
    int tempSymbol;
    in.read((char*) (&tempSize), sizeof(int));
    size += sizeof(int);

    char* tempSequenceid = new char[tempSize];
    in.read(tempSequenceid, sizeof(char) * tempSize);
    size += sizeof(char) * tempSize;
    _sequenceid = string(tempSequenceid);
    delete [] tempSequenceid;
    in.read((char*) (&tempSize), sizeof(int));
    size += sizeof(int);
    char* tempSequence = new char[tempSize];
    in.read(tempSequence, sizeof(char) * tempSize);
    size += sizeof(char) * tempSize;
    _sequence = string(tempSequence);
    _size = _sequence.size();
    delete [] tempSequence;
    in.read((char*) (&tempSize), sizeof(int));
    size += sizeof(int);
    for (int i=0; i<tempSize; ++i)
	{
        in.read((char*) (&tempSymbol), sizeof(int));
        _symbolIDs.push_back(tempSymbol);
        size += sizeof(int);
    }
    return size;
}
*/

string CDNAClass::getSequence()
{
	return _sequence;
}

/**
 * @brief return the name of a instance of this class
 * @return return the name of a instance of this class
*/
CreateFuntion CDNAClass::getConstructor()
{
    return &CreateInstance;
}

void* CDNAClass::CreateInstance()
{
    return new CDNAClass();
}

