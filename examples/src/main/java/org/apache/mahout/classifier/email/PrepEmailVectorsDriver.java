/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.classifier.email;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.mahout.math.VectorWritable;

import java.util.List;
import java.util.Map;

/**
 * Convert the labels generated by {@link org.apache.mahout.text.SequenceFilesFromMailArchives} and
 * {@link org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles} to ones consumable by the classifiers. We do this
 * here b/c if it is done in the creation of sparse vectors, the Reducer collapses all the vectors.
 */
public class PrepEmailVectorsDriver extends AbstractJob {

  public static final String ITEMS_PER_CLASS = "itemsPerClass";
  public static final String USE_LIST_NAME = "USE_LIST_NAME";

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(), new PrepEmailVectorsDriver(), args);
  }

  @Override
  public int run(String[] args) throws Exception {
    addInputOption();
    addOutputOption();
    addOption(DefaultOptionCreator.overwriteOption().create());
    addOption("maxItemsPerLabel", "mipl", "The maximum number of items per label.  Can be useful for making the "
        + "training sets the same size", String.valueOf(100000));
    addOption(buildOption("useListName", "ul", "Use the name of the list as part of the label.  If not set, then "
        + "just use the project name", false, false, "false"));
    Map<String,List<String>> parsedArgs = parseArguments(args);
    if (parsedArgs == null) {
      return -1;
    }

    Path input = getInputPath();
    Path output = getOutputPath();
    if (hasOption(DefaultOptionCreator.OVERWRITE_OPTION)) {
      HadoopUtil.delete(getConf(), output);
    }
    Job convertJob = prepareJob(input, output, SequenceFileInputFormat.class, PrepEmailMapper.class, Text.class,
        VectorWritable.class, PrepEmailReducer.class, Text.class, VectorWritable.class, SequenceFileOutputFormat.class);
    convertJob.getConfiguration().set(ITEMS_PER_CLASS, getOption("maxItemsPerLabel"));
    convertJob.getConfiguration().set(USE_LIST_NAME, String.valueOf(hasOption("useListName")));

    boolean succeeded = convertJob.waitForCompletion(true);
    return succeeded ? 0 : -1;
  }
}
