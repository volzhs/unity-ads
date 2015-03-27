#include <jni.h>

#ifndef DEBUGMODE
#define DEBUGMODE 0
#endif
#define NULL 0

static JavaVM* adsJavaVm;

class UnityAdsDalvikAttachThreadScoped
{
public:
	UnityAdsDalvikAttachThreadScoped(const char* sentinel)
	{
		m_detached = adsJavaVm->GetEnv((void **)&m_env, JNI_VERSION_1_2) == JNI_EDETACHED;
		if (m_detached)
		{
			if (DEBUGMODE && sentinel)
				__android_log_print(ANDROID_LOG_DEBUG, "UnityAds", "WARNING; Temporarily attached current thread to DalvikVM! (@ %s)", sentinel);
			adsJavaVm->AttachCurrentThread (&m_env, NULL);
		}
	#if DEBUGMODE
		sentinel = sentinel ? sentinel : "JNI Exception";
		strncpy(exception, sentinel, sizeof(exception) - 1);
		exception[sizeof(exception) - 1] = 0;
	#endif
		CheckException();
	}
	~UnityAdsDalvikAttachThreadScoped()
	{
		CheckException();
		if (m_detached)
			adsJavaVm->DetachCurrentThread();
	}
	inline operator JNIEnv* ()
	{
		CheckException();
		return m_env;
	}
	inline JNIEnv* operator -> ()
	{
		CheckException();
		return m_env;
	}
	inline bool operator ! ()
	{
		CheckException();
		return (m_env == 0);
	}
private:
#if DEBUGMODE
	void CheckException()
	{
		if (!m_env->ExceptionOccurred())
			return;

		printf_console("%s: Java exception detected", exception);
		m_env->ExceptionDescribe();
	}
	char	exception[16];
#else
	void CheckException() {}
#endif
	bool	m_detached;
	JNIEnv*	m_env;
};
