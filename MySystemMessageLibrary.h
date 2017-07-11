
#ifndef _MYSYSTEMMESSAGELIBRARY_H_
#define _MYSYSTEMMESSAGELIBRARY_H_

//system defined messages
typedef list<DPNMSG_RECEIVE> PlainMessageList;
typedef list<DPNMSG_CONNECT_COMPLETE> ConnectionResponseMessageList;
typedef list<DPNMSG_SEND_COMPLETE> SendCompleteMessageList;
typedef list<DPNMSG_SERVER_INFO> ServerInfoChangedMessageList;
typedef list<DPNMSG_TERMINATE_SESSION> TerminateSessionMessageList;
typedef list<DPNMSG_ASYNC_OP_COMPLETE> AsycOpCompleteMessageList;
typedef list<DPNMSG_RETURN_BUFFER> ReturnBufferMessageList;

class MYSYSTEMMESSAGELIBRARY
{
private:
	PlainMessageList m_MyPlainMessages;
	PlainMessageList::const_iterator m_PML_ci;
	PlainMessageList::iterator m_PML_nci;

	ConnectionResponseMessageList m_MyConnectionResponseMessages;
	ConnectionResponseMessageList::const_iterator m_CRML_ci;
	ConnectionResponseMessageList::iterator m_CRML_nci;

	SendCompleteMessageList m_MySendCompleteMessages;
	SendCompleteMessageList::const_iterator m_SCML_ci;
	SendCompleteMessageList::iterator m_SCML_nci;

	ServerInfoChangedMessageList m_MyServerInfoChangedMessages;
	ServerInfoChangedMessageList::const_iterator m_SICML_ci;
	ServerInfoChangedMessageList::iterator m_SICML_nci;

	TerminateSessionMessageList m_MyTerminateSessionMessages;
	TerminateSessionMessageList::const_iterator m_TSML_ci;
	TerminateSessionMessageList::iterator m_TSML_nci;

	AsycOpCompleteMessageList m_MyAsycOpCompleteMessages;
	AsycOpCompleteMessageList::const_iterator m_AOCML_ci;
	AsycOpCompleteMessageList::iterator m_AOCML_nci;

	ReturnBufferMessageList m_MyReturnBufferMessages;
	ReturnBufferMessageList::const_iterator m_RBML_ci;
	ReturnBufferMessageList::iterator m_RBML_nci;


public:
	MYSYSTEMMESSAGELIBRARY();
	~MYSYSTEMMESSAGELIBRARY();

	void* Add_Message_DPNMSG_RECEIVE(void* vpMessage);
	DPNMSG_RECEIVE Get_Message_DPNMSG_RECEIVE(void);

	void* Add_Message_DPNMSG_CONNECT_COMPLETE(void* vpMessage);
	DPNMSG_CONNECT_COMPLETE Get_Message_DPNMSG_CONNECT_COMPLETE(void);

	void* Add_Message_DPNMSG_SEND_COMPLETE(void* vpMessage);
	DPNMSG_SEND_COMPLETE Get_Message_DPNMSG_SEND_COMPLETE(void);

	void* Add_Message_DPNMSG_SERVER_INFO(void* vpMessage);
	DPNMSG_SERVER_INFO Get_Message_DPNMSG_SERVER_INFO(void);

	void* Add_Message_DPNMSG_TERMINATE_SESSION(void* vpMessage);
	DPNMSG_TERMINATE_SESSION Get_Message_DPNMSG_TERMINATE_SESSION(void);

	void* Add_Message_DPNMSG_ASYNC_OP_COMPLETE(void* vpMessage);
	DPNMSG_ASYNC_OP_COMPLETE Get_Message_DPNMSG_ASYNC_OP_COMPLETE(void);

	void* Add_Message_DPNMSG_RETURN_BUFFER(void* vpMessage);
	DPNMSG_RETURN_BUFFER Get_Message_DPNMSG_RETURN_BUFFER(void);

};

MYSYSTEMMESSAGELIBRARY::MYSYSTEMMESSAGELIBRARY()
{

	m_PML_ci = m_MyPlainMessages.begin();
	m_PML_nci = m_MyPlainMessages.begin();

	m_CRML_ci = m_MyConnectionResponseMessages.begin();
	m_CRML_nci = m_MyConnectionResponseMessages.begin();

	m_SCML_ci = m_MySendCompleteMessages.begin();
	m_SCML_nci = m_MySendCompleteMessages.begin();
	
	m_SICML_ci = m_MyServerInfoChangedMessages.begin();
	m_SICML_nci = m_MyServerInfoChangedMessages.begin();
	
	m_TSML_ci = m_MyTerminateSessionMessages.begin();
	m_TSML_nci = m_MyTerminateSessionMessages.begin();

	m_AOCML_ci = m_MyAsycOpCompleteMessages.begin();
	m_AOCML_nci = m_MyAsycOpCompleteMessages.begin();

	m_RBML_ci = m_MyReturnBufferMessages.begin();
	m_RBML_nci = m_MyReturnBufferMessages.begin();
}

MYSYSTEMMESSAGELIBRARY::~MYSYSTEMMESSAGELIBRARY()
{
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_RECEIVE(void* vpMessage)
{
	//convert data to DPNMSG_RECEIVE
	DPNMSG_RECEIVE* pDPMessage = (DPNMSG_RECEIVE*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MyPlainMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_PML_nci = m_MyPlainMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_PML_nci);
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_CONNECT_COMPLETE(void* vpMessage)
{
	//convert data to DPNMSG_CONNECT_COMPLETE
	DPNMSG_CONNECT_COMPLETE* pDPMessage = (DPNMSG_CONNECT_COMPLETE*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MyConnectionResponseMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_CRML_nci = m_MyConnectionResponseMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_CRML_nci);
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_SEND_COMPLETE(void* vpMessage)
{
	//convert data to DPNMSG_SEND_COMPLETE
	DPNMSG_SEND_COMPLETE* pDPMessage = (DPNMSG_SEND_COMPLETE*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MySendCompleteMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_SCML_nci = m_MySendCompleteMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_SCML_nci);
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_SERVER_INFO(void* vpMessage)
{
	//convert data to DPNMSG_SERVER_INFO
	DPNMSG_SERVER_INFO* pDPMessage = (DPNMSG_SERVER_INFO*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MyServerInfoChangedMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_SICML_nci = m_MyServerInfoChangedMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_SICML_nci);
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_TERMINATE_SESSION(void* vpMessage)
{
	//convert data to DPNMSG_TERMINATE_SESSION
	DPNMSG_TERMINATE_SESSION* pDPMessage = (DPNMSG_TERMINATE_SESSION*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MyTerminateSessionMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_TSML_nci = m_MyTerminateSessionMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_TSML_nci);
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_ASYNC_OP_COMPLETE(void* vpMessage)
{
	//convert data to DPNMSG_ASYNC_OP_COMPLETE
	DPNMSG_ASYNC_OP_COMPLETE* pDPMessage = (DPNMSG_ASYNC_OP_COMPLETE*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MyAsycOpCompleteMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_AOCML_nci = m_MyAsycOpCompleteMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_AOCML_nci);
}

void* MYSYSTEMMESSAGELIBRARY::Add_Message_DPNMSG_RETURN_BUFFER(void* vpMessage)
{
	//convert data to DPNMSG_RETURN_BUFFER
	DPNMSG_RETURN_BUFFER* pDPMessage = (DPNMSG_RETURN_BUFFER*) vpMessage;

	//store a local copy of the message
	//at the top of the stack
	m_MyReturnBufferMessages.push_front(*pDPMessage);

	//set the iterator to the top of the stack
	m_RBML_nci = m_MyReturnBufferMessages.begin();

	//release local pointers
	pDPMessage = NULL;

	//return a pointer to the stored structure
	return (void*)&(*m_RBML_nci);
}

MYSYSTEMMESSAGELIBRARY MySystemMessageLibrary;

#endif //_MYSYSTEMMESSAGELIBRARY_H_