package dev.skarch.ai_logpanel.data

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream

class SshRepository {

    private var session: Session? = null

    fun connect(user: String, host: String, port: Int, privateKeyPath: String) {
        val jsch = JSch()
        jsch.addIdentity(privateKeyPath)
        session = jsch.getSession(user, host, port)
        session?.setConfig("StrictHostKeyChecking", "no")
        session?.connect()
    }

    fun disconnect() {
        session?.disconnect()
    }

    fun tailLog(logPath: String): Flow<String> = flow {
        val channel = session?.openChannel("exec") as ChannelExec
        channel.setCommand("tail -f $logPath")
        val inputStream = channel.inputStream
        channel.connect()

        inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                emit(line)
            }
        }
    }.flowOn(Dispatchers.IO)
}
