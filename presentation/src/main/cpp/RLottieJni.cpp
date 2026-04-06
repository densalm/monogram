#include <jni.h>

#include <string>

#include "RLottieDecoder.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_create(JNIEnv *, jobject) {
    return reinterpret_cast<jlong>(new RLottieDecoder());
}

JNIEXPORT jboolean JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_openFromData(
    JNIEnv *env,
    jobject,
    jlong ptr,
    jstring json,
    jstring cacheKey,
    jstring resourcePath
) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    if (decoder == nullptr || json == nullptr || cacheKey == nullptr || resourcePath == nullptr) {
        return JNI_FALSE;
    }

    const char *jsonChars = env->GetStringUTFChars(json, nullptr);
    const char *keyChars = env->GetStringUTFChars(cacheKey, nullptr);
    const char *pathChars = env->GetStringUTFChars(resourcePath, nullptr);

    const std::string jsonData(jsonChars != nullptr ? jsonChars : "");
    const std::string keyData(keyChars != nullptr ? keyChars : "");
    const std::string pathData(pathChars != nullptr ? pathChars : "");

    if (jsonChars != nullptr) env->ReleaseStringUTFChars(json, jsonChars);
    if (keyChars != nullptr) env->ReleaseStringUTFChars(cacheKey, keyChars);
    if (pathChars != nullptr) env->ReleaseStringUTFChars(resourcePath, pathChars);

    return decoder->openFromData(jsonData, keyData, pathData) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_renderFrame(
    JNIEnv *env,
    jobject,
    jlong ptr,
    jobject bitmap,
    jint frameNo,
    jint drawLeft,
    jint drawTop,
    jint drawWidth,
    jint drawHeight
) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    if (decoder == nullptr) {
        return JNI_FALSE;
    }

    return decoder->renderFrame(env, bitmap, frameNo, drawLeft, drawTop, drawWidth, drawHeight)
        ? JNI_TRUE
        : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getWidth(JNIEnv *, jobject, jlong ptr) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    return decoder == nullptr ? 0 : decoder->getWidth();
}

JNIEXPORT jint JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getHeight(JNIEnv *, jobject, jlong ptr) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    return decoder == nullptr ? 0 : decoder->getHeight();
}

JNIEXPORT jint JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getTotalFrames(JNIEnv *, jobject, jlong ptr) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    return decoder == nullptr ? 0 : decoder->getTotalFrames();
}

JNIEXPORT jdouble JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getFrameRate(JNIEnv *, jobject, jlong ptr) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    return decoder == nullptr ? 0.0 : decoder->getFrameRate();
}

JNIEXPORT jlong JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getDurationMs(JNIEnv *, jobject, jlong ptr) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    return decoder == nullptr ? 0 : decoder->getDurationMs();
}

JNIEXPORT void JNICALL
Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_destroy(JNIEnv *, jobject, jlong ptr) {
    auto *decoder = reinterpret_cast<RLottieDecoder *>(ptr);
    delete decoder;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK || env == nullptr) {
        return JNI_ERR;
    }

    jclass wrapperClass = env->FindClass("org/monogram/presentation/features/stickers/core/RLottieWrapper");
    if (wrapperClass == nullptr) {
        return JNI_ERR;
    }

    static const JNINativeMethod methods[] = {
        {const_cast<char *>("create"), const_cast<char *>("()J"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_create)},
        {const_cast<char *>("openFromData"), const_cast<char *>("(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_openFromData)},
        {const_cast<char *>("renderFrame"), const_cast<char *>("(JLandroid/graphics/Bitmap;IIIII)Z"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_renderFrame)},
        {const_cast<char *>("getWidth"), const_cast<char *>("(J)I"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getWidth)},
        {const_cast<char *>("getHeight"), const_cast<char *>("(J)I"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getHeight)},
        {const_cast<char *>("getTotalFrames"), const_cast<char *>("(J)I"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getTotalFrames)},
        {const_cast<char *>("getFrameRate"), const_cast<char *>("(J)D"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getFrameRate)},
        {const_cast<char *>("getDurationMs"), const_cast<char *>("(J)J"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_getDurationMs)},
        {const_cast<char *>("destroy"), const_cast<char *>("(J)V"), reinterpret_cast<void *>(Java_org_monogram_presentation_features_stickers_core_RLottieWrapper_destroy)}
    };

    constexpr jint methodCount = static_cast<jint>(sizeof(methods) / sizeof(methods[0]));
    if (env->RegisterNatives(wrapperClass, methods, methodCount) != JNI_OK) {
        env->DeleteLocalRef(wrapperClass);
        return JNI_ERR;
    }

    env->DeleteLocalRef(wrapperClass);
    return JNI_VERSION_1_6;
}

}
