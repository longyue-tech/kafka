/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.requests;

import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.message.DescribeAclsRequestData;
import org.apache.kafka.common.message.DescribeAclsResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import java.nio.ByteBuffer;


public class DescribeAclsRequest extends AbstractRequest {

    public static class Builder extends AbstractRequest.Builder<DescribeAclsRequest> {
        private final DescribeAclsRequestData data;

        public Builder(AclBindingFilter filter) {
            super(ApiKeys.DESCRIBE_ACLS);
            ResourcePatternFilter patternFilter = filter.patternFilter();
            AccessControlEntryFilter entryFilter = filter.entryFilter();
            data = new DescribeAclsRequestData()
                .setHostFilter(entryFilter.host())
                .setOperation(entryFilter.operation().code())
                .setPermissionType(entryFilter.permissionType().code())
                .setPrincipalFilter(entryFilter.principal())
                .setResourceNameFilter(patternFilter.name())
                .setResourcePatternType(patternFilter.patternType().code())
                .setResourceType(patternFilter.resourceType().code());
        }

        @Override
        public DescribeAclsRequest build(short version) {
            return new DescribeAclsRequest(data, version);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    private final DescribeAclsRequestData data;

    public DescribeAclsRequest(DescribeAclsRequestData data, short version) {
        super(ApiKeys.DESCRIBE_ACLS, version);
        this.data = data;
        validate(version);
    }

    public DescribeAclsRequest(Struct struct, short version) {
        super(ApiKeys.DESCRIBE_ACLS, version);
        this.data = new DescribeAclsRequestData(struct, version);
    }

    public DescribeAclsRequestData data() {
        return data;
    }

    @Override
    protected Struct toStruct() {
        return data.toStruct(version());
    }

    @Override
    public AbstractResponse getErrorResponse(int throttleTimeMs, Throwable throwable) {
        ApiError error = ApiError.fromThrowable(throwable);
        DescribeAclsResponseData response = new DescribeAclsResponseData()
            .setThrottleTimeMs(throttleTimeMs)
            .setErrorCode(error.error().code())
            .setErrorMessage(error.message());
        return new DescribeAclsResponse(response);
    }

    public static DescribeAclsRequest parse(ByteBuffer buffer, short version) {
        return new DescribeAclsRequest(ApiKeys.DESCRIBE_ACLS.parseRequest(version, buffer), version);
    }

    public AclBindingFilter filter() {
        ResourcePatternFilter rpf = new ResourcePatternFilter(
                ResourceType.fromCode(data.resourceType()),
                data.resourceNameFilter(),
                PatternType.fromCode(data.resourcePatternType()));
        AccessControlEntryFilter acef =  new AccessControlEntryFilter(
                data.principalFilter(),
                data.hostFilter(),
                AclOperation.fromCode(data.operation()),
                AclPermissionType.fromCode(data.permissionType()));
        return new AclBindingFilter(rpf, acef);
    }

    private void validate(short version) {
        if (version == 0) {
            if (data.resourcePatternType() == PatternType.ANY.code()) {
                data.setResourcePatternType(PatternType.LITERAL.code());
            }
            if (data.resourcePatternType() != PatternType.LITERAL.code()) {
                throw new UnsupportedVersionException("Version 0 only supports literal resource pattern types");
            }
        }

        if (data.resourcePatternType() == PatternType.UNKNOWN.code()
            || data.resourceType() == ResourceType.UNKNOWN.code()
            || data.permissionType() == AclPermissionType.UNKNOWN.code()
            || data.operation() == AclOperation.UNKNOWN.code()) {
            throw new IllegalArgumentException("Filter contain UNKNOWN elements");
        }
    }

}
