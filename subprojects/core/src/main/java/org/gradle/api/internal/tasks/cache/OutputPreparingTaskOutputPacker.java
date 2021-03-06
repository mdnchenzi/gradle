/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.cache;

import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.TaskOutputsInternal;
import org.gradle.api.internal.tasks.cache.origin.OriginMetadataProcessor;
import org.gradle.api.internal.tasks.properties.CacheableTaskOutputFilePropertySpec;
import org.gradle.api.internal.tasks.properties.TaskOutputFilePropertySpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Prepares a task's outputs to be loaded from cache: removes any previous output and makes sure the output directories exist.
 */
public class OutputPreparingTaskOutputPacker implements TaskOutputPacker {
    private final TaskOutputPacker delegate;

    public OutputPreparingTaskOutputPacker(TaskOutputPacker delegate) {
        this.delegate = delegate;
    }

    @Override
    public void pack(OriginMetadata originMetadata, TaskOutputsInternal taskOutputs, OutputStream output) throws IOException {
        delegate.pack(originMetadata, taskOutputs, output);
    }

    @Override
    public void unpack(OriginMetadataProcessor processor, TaskOutputsInternal taskOutputs, InputStream input) throws IOException {
        for (TaskOutputFilePropertySpec propertySpec : taskOutputs.getFileProperties()) {
            CacheableTaskOutputFilePropertySpec property = (CacheableTaskOutputFilePropertySpec) propertySpec;
            File output = property.getOutputFile();
            if (output == null) {
                continue;
            }
            switch (property.getOutputType()) {
                case DIRECTORY:
                    makeDirectory(output);
                    FileUtils.cleanDirectory(output);
                    break;
                case FILE:
                    if (!makeDirectory(output.getParentFile())) {
                        if (output.exists()) {
                            FileUtils.forceDelete(output);
                        }
                    }
                    break;
                default:
                    throw new AssertionError();
            }
        }
        delegate.unpack(processor, taskOutputs, input);
    }

    private static boolean makeDirectory(File output) throws IOException {
        if (output.isDirectory()) {
            return false;
        } else if (output.isFile()) {
            FileUtils.forceDelete(output);
        }
        FileUtils.forceMkdir(output);
        return true;
    }
}
