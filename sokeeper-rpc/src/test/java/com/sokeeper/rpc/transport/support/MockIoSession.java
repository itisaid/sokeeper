/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package com.sokeeper.rpc.transport.support;

import java.net.SocketAddress;
import java.util.Set;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TrafficMask;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;

/**
 * @author James Fu (fuyinhai@gmail.com)
 */
public class MockIoSession implements IoSession {

    private SocketAddress    localAddress;
    private SocketAddress    remoteAddress;
    private Object           writedMessage;
    private RuntimeException writeMessageException;

    public void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setLocalAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public CloseFuture close() {
        return null;
    }

    public boolean containsAttribute(String key) {
        return false;
    }

    public Object getAttachment() {
        return null;
    }

    public Object getAttribute(String key) {
        return null;
    }

    public Set<String> getAttributeKeys() {
        return null;
    }

    public CloseFuture getCloseFuture() {
        return null;
    }

    public IoSessionConfig getConfig() {
        return null;
    }

    public long getCreationTime() {
        return 0;
    }

    public IoFilterChain getFilterChain() {
        return null;
    }

    public IoHandler getHandler() {
        return null;
    }

    public int getIdleCount(IdleStatus status) {
        return 0;
    }

    public int getIdleTime(IdleStatus status) {

        return 0;
    }

    public long getIdleTimeInMillis(IdleStatus status) {

        return 0;
    }

    public long getLastIdleTime(IdleStatus status) {

        return 0;
    }

    public long getLastIoTime() {

        return 0;
    }

    public long getLastReadTime() {

        return 0;
    }

    public long getLastWriteTime() {

        return 0;
    }

    public SocketAddress getLocalAddress() {

        return this.localAddress;
    }

    public long getReadBytes() {

        return 0;
    }

    public long getReadMessages() {

        return 0;
    }

    public SocketAddress getRemoteAddress() {

        return remoteAddress;
    }

    public int getScheduledWriteBytes() {

        return 0;
    }

    public int getScheduledWriteRequests() {

        return 0;
    }

    public IoService getService() {

        return null;
    }

    public SocketAddress getServiceAddress() {

        return null;
    }

    public IoServiceConfig getServiceConfig() {

        return null;
    }

    public TrafficMask getTrafficMask() {

        return null;
    }

    public TransportType getTransportType() {

        return null;
    }

    public int getWriteTimeout() {

        return 0;
    }

    public long getWriteTimeoutInMillis() {

        return 0;
    }

    public long getWrittenBytes() {

        return 0;
    }

    public long getWrittenMessages() {

        return 0;
    }

    public long getWrittenWriteRequests() {

        return 0;
    }

    public boolean isClosing() {

        return false;
    }

    public boolean isConnected() {

        return false;
    }

    public boolean isIdle(IdleStatus status) {

        return false;
    }

    public Object removeAttribute(String key) {

        return null;
    }

    public void resumeRead() {

    }

    public void resumeWrite() {

    }

    public Object setAttachment(Object attachment) {

        return null;
    }

    public Object setAttribute(String key) {

        return null;
    }

    public Object setAttribute(String key, Object value) {

        return null;
    }

    public void setIdleTime(IdleStatus status, int idleTime) {

    }

    public void setTrafficMask(TrafficMask trafficMask) {

    }

    public void setWriteTimeout(int writeTimeout) {

    }

    public void suspendRead() {

    }

    public void suspendWrite() {

    }

    public WriteFuture write(Object message) {
        if (writeMessageException != null) {
            throw writeMessageException;
        }
        writedMessage = message;
        return null;
    }

    public Object getWritedMessage() {
        return writedMessage;
    }

    public void setWriteMessageException(RuntimeException writeMessageException) {
        this.writeMessageException = writeMessageException;
    }

}
