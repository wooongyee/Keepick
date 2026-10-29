package com.ssafy.keepick.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class UserTokenSerializer @Inject constructor() : Serializer<UserToken> {

    /**
     * DataStore가 처음 생성될 때 사용할 기본값을 정의합니다.
     * 파일이 아직 없으면 이 값이 사용됩니다.
     */
    override val defaultValue: UserToken = UserToken.getDefaultInstance()

    /**
     * InputStream에서 데이터를 읽어 UserToken 객체로 변환(역직렬화)하는 방법을 정의합니다.
     * @param input DataStore 파일의 InputStream
     * @return 파싱된 UserToken 객체
     * @throws CorruptionException 데이터가 손상되어 파싱할 수 없을 때 발생
     */
    override suspend fun readFrom(input: InputStream): UserToken {
        try {
            // protoc가 생성한 parseFrom 메서드를 사용하여 InputStream으로부터 객체를 파싱합니다.
            return UserToken.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            // 프로토콜 버퍼 파싱 오류 발생 시, DataStore가 처리할 수 있는 예외로 변환하여 던집니다.
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    /**
     * UserToken 객체를 OutputStream에 쓰는(직렬화) 방법을 정의합니다.
     * @param t 저장할 UserToken 객체
     * @param output DataStore 파일의 OutputStream
     */
    override suspend fun writeTo(t: UserToken, output: OutputStream) {
        // protoc가 생성한 writeTo 메서드를 사용하여 객체를 OutputStream에 씁니다.
        t.writeTo(output)
    }
}
