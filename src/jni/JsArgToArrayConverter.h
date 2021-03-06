#ifndef JSARGTOARRAYCONVERTER_H_
#define JSARGTOARRAYCONVERTER_H_

#include "JEnv.h"
#include "v8.h"
#include <vector>
#include <string>

namespace tns
{
	class JsArgToArrayConverter
	{
	public:
		JsArgToArrayConverter(const v8::FunctionCallbackInfo<v8::Value>& args, bool hasImplementationObject, bool isInnerClass = false);

		JsArgToArrayConverter(const v8::Handle<v8::Value>& arg, bool isImplementationObject);

		~JsArgToArrayConverter();

		jobjectArray ToJavaArray();

		jobject GetConvertedArg();

		int Length() const;

		bool IsValid() const;

		struct Error;

		Error GetError() const;

		static void Init(JavaVM *jvm);

		struct Error
		{
			Error() : index(-1), msg(std::string())
			{}

			int index;
			std::string msg;
		};

	private:
		bool ConvertArg(const v8::Handle<v8::Value>& arg, int index);

		void SetConvertedObject(JEnv& env, int index, jobject obj, bool isGlobalRef = false);

		static JavaVM *m_jvm;

		int m_argsLen;

		bool m_isValid;

		Error m_error;

		std::vector<int> m_storedIndexes;

		jobject *m_argsAsObject;

		jobjectArray m_arr;

		static jclass STRING_CLASS;

		static jmethodID STRING_CTOR;

		static jstring UTF_8_ENCODING;
	};
}


#endif /* JSARGTOARRAYCONVERTER_H_ */
