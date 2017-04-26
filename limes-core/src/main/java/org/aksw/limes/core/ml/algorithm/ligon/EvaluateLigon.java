/**
 * 
 */
package org.aksw.limes.core.ml.algorithm.ligon;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.aksw.limes.core.datastrutures.GoldStandard;
import org.aksw.limes.core.evaluation.evaluationDataLoader.DataSetChooser;
import org.aksw.limes.core.evaluation.evaluationDataLoader.DataSetChooser.DataSets;
import org.aksw.limes.core.evaluation.evaluationDataLoader.EvaluationData;
import org.aksw.limes.core.evaluation.qualititativeMeasures.FMeasure;
import org.aksw.limes.core.evaluation.qualititativeMeasures.Precision;
import org.aksw.limes.core.evaluation.qualititativeMeasures.Recall;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.execution.engine.ExecutionEngine;
import org.aksw.limes.core.execution.engine.ExecutionEngineFactory;
import org.aksw.limes.core.execution.engine.ExecutionEngineFactory.ExecutionEngineType;
import org.aksw.limes.core.execution.planning.planner.ExecutionPlannerFactory;
import org.aksw.limes.core.execution.planning.planner.ExecutionPlannerFactory.ExecutionPlannerType;
import org.aksw.limes.core.execution.planning.planner.IPlanner;
import org.aksw.limes.core.execution.rewriter.Rewriter;
import org.aksw.limes.core.execution.rewriter.RewriterFactory;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.HybridCache;
import org.aksw.limes.core.io.ls.LinkSpecification;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.measures.mapper.MappingOperations;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.algorithm.FuzzyWombatSimple;
import org.aksw.limes.core.ml.algorithm.MLAlgorithmFactory;
import org.aksw.limes.core.ml.algorithm.MLImplementationType;
import org.aksw.limes.core.ml.algorithm.MLResults;
import org.aksw.limes.core.ml.algorithm.SupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.WombatSimple;
import org.apache.log4j.Logger;




/**
 * Evaluate Refinement based LGG for benchmark datasets
 * DBLP-ACM, Abt-Buy,Amazon-GoogleProducts, DBLP-Scholar, 
 * Person1, Person2, Restaurants, DBLP-LinkedMDB and Dailymed-DrugBank
 * 
 * @author sherif
 * 
 */
/**
 * @author Mohamed Sherif (sherif@informatik.uni-leipzig.de)
 *
 */
/**
 * @author Mohamed Sherif (sherif@informatik.uni-leipzig.de)
 *
 */
public class EvaluateLigon extends FuzzyWombatSimple{
    /**
     * 
     */
    private static final Logger logger = Logger.getLogger(EvaluateLigon.class);
    protected static final double MIN_COVERAGE = 0.6;

    public static ACache fullSourceCache;
    public static ACache sourceTrainCache = new HybridCache();
    public static ACache sourceTestCache = new HybridCache();
    public static ACache fullTargetCache;
    public static ACache targetTrainCache = new HybridCache();
    public static ACache targetTestCache = new HybridCache();
    public static AMapping fullReferenceMapping = MappingFactory.createDefaultMapping();
    public static String resultStr = new String();


    /**
     * Computes a sample of the reference dataset with size equal to the the given fraction of the reference dataset
     * @param reference dataset
     * @param fraction of the reference dataset (sample size) 
     * @return
     */
    public static AMapping sampleReferenceMap(AMapping reference, double fraction) {
        if(fraction == 1){
            return reference;
        }
        //		int mapSize = reference.size();
        if (fraction > 1) {
            fraction = 1 / fraction;
        }
        int size = (int) (reference.getMap().keySet().size() * fraction);
        return sampleReferenceMap(reference, size);
    }

    /**
     * Computes a sample of the reference dataset with the given size
     * @param reference
     * @param size
     * @return
     */
    public static AMapping sampleReferenceMap(AMapping reference, int size) {
        Set<Integer> index = new HashSet<>();
        //get random indexes
        for (int i = 0; i < size; i++) {
            int number;
            do {
                number = (int) (reference.getMap().keySet().size() * Math.random());
            } while (index.contains(number));
            index.add(number);
        } 

        //get data
        AMapping sample = MappingFactory.createDefaultMapping();		
        int count = 0;
        for (String key : reference.getMap().keySet()) {
            if (index.contains(count)) {
                sample.getMap().put(key, reference.getMap().get(key));
            }
            count++;
        }

        // compute sample size
        for (String key : sample.getMap().keySet()) {
            for (String value : sample.getMap().get(key).keySet()) {
                sample.setSize(size++);
            }
        }
        return sample;
    }

    public static AMapping sampleReference(AMapping reference, float start, float end) {
        if(start == 0 && end == reference.size()){
            return reference;
        }
        int count = 0;
        AMapping sample = MappingFactory.createDefaultMapping();
        for (String key : reference.getMap().keySet()) {
            for (String value : reference.getMap().get(key).keySet()) {
                if(count < start*reference.size()){
                    count++;
                }else{
                    sample.add(key, value, reference.getMap().get(key).get(value));
                    sample.setSize(sample.getSize() + 1);
                    count++;
                }
                if(count >= end*reference.size()){
                    return sample;
                }
            }
        }
        return null;
    }

    /**
     * Extract the source and target training cache instances based on the input learnMap
     * @param learnMap to be used for training caches filling
     * @author sherif
     */
    protected static void fillTrainingCaches(AMapping learnMap) {
        if (learnMap.size() == fullReferenceMapping.size()){
            sourceTrainCache = fullSourceCache;
            targetTrainCache = fullTargetCache;
        }else{
            sourceTrainCache = new HybridCache();
            targetTrainCache = new HybridCache();
            for (String s : learnMap.getMap().keySet()) {
                if(fullSourceCache.containsUri(s)){
                    sourceTrainCache.addInstance(fullSourceCache.getInstance(s));
                    for (String t : learnMap.getMap().get(s).keySet()) {
                        if(fullTargetCache.containsUri(t)){
                            targetTrainCache.addInstance(fullTargetCache.getInstance(t));
                        }else{
                            logger.warn("Instance " + t + " not exist in the target dataset");
                        }
                    }
                }else{
                    logger.warn("Instance " + s + " not exist in the source dataset");
                }
            }
        }
    }


    /**
     * Extract the source and target testing cache instances based on the input trainMap
     * @param trainMap to be used for testing caches filling
     * @author sherif
     */
    protected static void fillTestingCaches(AMapping trainMap) {
        if (trainMap.size() == fullReferenceMapping.size()){
            sourceTestCache = fullSourceCache;
            targetTestCache = fullTargetCache;
        }else{
            sourceTestCache = new HybridCache();
            targetTestCache = new HybridCache();
            for (String s : trainMap.getMap().keySet()) {
                if(fullSourceCache.containsUri(s)){
                    sourceTestCache.addInstance(fullSourceCache.getInstance(s));
                    for (String t : trainMap.getMap().get(s).keySet()) {
                        if(fullTargetCache.containsUri(t)){
                            targetTestCache.addInstance(fullTargetCache.getInstance(t));
                        }else{
                            logger.warn("Instance " + t + " not exist in the target dataset");
                        }
                    }
                }else{
                    logger.warn("Instance " + s + " not exist in the source dataset");
                }
            }
        }
    }



    /**
     * Remove AMapping entries with missing source or target instances
     * @param map input map
     * @author sherif
     */
    protected static AMapping removeLinksWithNoInstances(AMapping map) {
        AMapping result = MappingFactory.createDefaultMapping();
        for (String s : map.getMap().keySet()) {
            for (String t : map.getMap().get(s).keySet()) {
                if(fullSourceCache.containsUri(s) && fullTargetCache.containsUri(t)){
                    result.add(s,t, map.getMap().get(s).get(t));
                }
            }
        }
        return result;
    }


    /**
     * @param d dataset used in evaluation
     * @param posExFrac fraction of training data
     * @throws UnsupportedMLImplementationException
     */
    private static String evaluateFuzzyWombat(DataSets d, int posExFrac) throws UnsupportedMLImplementationException {

        resultStr +=  d +"\n" +
                "Sample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n";
        EvaluationData data = DataSetChooser.getData(d);
        fullSourceCache = data.getSourceCache();
        fullTargetCache = data.getTargetCache();
        fullReferenceMapping = data.getReferenceMapping();

        // remove error mappings (if any)
        int refMapSize = fullReferenceMapping.size();
        fullReferenceMapping = removeLinksWithNoInstances(fullReferenceMapping);

        logger.info("Number of removed error mappings = " + (refMapSize - fullReferenceMapping.size()));
        //		System.out.println(reference.size());System.exit(1);
        if(posExFrac <= 0){ // learn using 10%, 20%, ... , 100%
            crossValidation10Fold(d);
        }else{ 
            percentageValidation(d, posExFrac);

        }
        System.out.println(d + " Final rasults:\n" + resultStr);
        return resultStr;
    }

    private static void percentageValidation(DataSets d, int posExFrac)
            throws UnsupportedMLImplementationException {
        // learn using provided leaningRat
        logger.info("Running Fuzzy Wombat for the " + d + " dataset with positive example size = " +  posExFrac + "%");

        AMapping trainingMap = sampleReferenceMap(fullReferenceMapping, posExFrac/100f);
        AMapping testMap  = MappingOperations.difference(fullReferenceMapping, trainingMap);               
        fillTrainingCaches(trainingMap);
        fillTestingCaches(testMap);
        trainingMap.getReversedMap();

        // 1. Learning phase
        logger.info("Learning using " + trainingMap.size() + " examples.");
        resultStr += posExFrac + "%\t" ;
        MLResults mlResult = trainFromSampleMapping(trainingMap);

        // 2. Apply for the whole KB
        executeLinkSpecs(mlResult.getLinkSpecification(), sourceTestCache, targetTestCache);
    }

    /**
     * @param d Dataset for cross validation
     * @throws UnsupportedMLImplementationException
     */
    private static void crossValidation10Fold(DataSets d)
            throws UnsupportedMLImplementationException {
        for(int s = 1 ; s <= 10 ; s +=1){
            logger.info("Running " + " Fuzzy Wombat for the " + d + " dataset with positive example size = " +  s*10 + "%");
            AMapping trainingMap = sampleReferenceMap(fullReferenceMapping, s/10f);
            AMapping testMap  = MappingOperations.difference(fullReferenceMapping, trainingMap);               
            fillTrainingCaches(trainingMap);
            fillTestingCaches(testMap);
            trainingMap.getReversedMap();

            // 1. Learning phase
            MLResults mlResult = trainFromSampleMapping(trainingMap);

            // 2. Apply for the whole KB
            executeLinkSpecs(mlResult.getLinkSpecification(), sourceTestCache, targetTestCache);
        }
    }	

    /**
     * @param trainingSample mapping for training
     * @throws UnsupportedMLImplementationException
     */
    static MLResults trainFromSampleMapping(AMapping trainingSample) throws UnsupportedMLImplementationException{
        long start = System.currentTimeMillis();
        SupervisedMLAlgorithm fuzzyWombat = null;
        try {
            fuzzyWombat = MLAlgorithmFactory
                    .createMLAlgorithm(FuzzyWombatSimple.class,
                            MLImplementationType.SUPERVISED_BATCH)
                    .asSupervised();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
            fail();
        }
        fuzzyWombat.init(null, sourceTrainCache, targetTrainCache);
        MLResults mlModel = fuzzyWombat.learn(trainingSample);
        AMapping learnedMap = fuzzyWombat.predict(sourceTrainCache, targetTrainCache, mlModel);
        learnedMap = AMapping.getBestOneToOneMappings(learnedMap);
        LinkSpecification linkSpecification = mlModel.getLinkSpecification();
        resultStr +=  
                //                posExFrac + "%"                         + "\t" + 
                precision(learnedMap, trainingSample)+ "\t" + 
                recall(learnedMap, trainingSample)   + "\t" + 
                fScore(learnedMap, trainingSample)   + "\t" +
                (System.currentTimeMillis() - start)            + "\t" +
                linkSpecification.toStringOneLine()                   + "\t" ;
        System.out.println("Training Mapping result: \n" + learnedMap);
        System.out.println("trainingSample: \n" + trainingSample);
        return mlModel;
    }


    /**
     * @param d dataset 
     * @param linkSpecification to be applied to the whole dataset d 
     * @return
     */
    static String executeLinkSpecs(LinkSpecification linkSpecification, ACache sourceCache, ACache targetCache){
        long start = System.currentTimeMillis();
        AMapping kbMap;
        Rewriter rw = RewriterFactory.getDefaultRewriter();
        LinkSpecification rwLs = rw.rewrite(linkSpecification);
        IPlanner planner = ExecutionPlannerFactory.getPlanner(ExecutionPlannerType.DEFAULT, sourceCache, targetCache);
        assert planner != null;
        ExecutionEngine engine = ExecutionEngineFactory.getEngine(ExecutionEngineType.DEFAULT, sourceCache, targetCache, "?x", "?y");
        assert engine != null;
        AMapping resultMap = engine.execute(rwLs, planner);
        kbMap = resultMap.getSubMap(linkSpecification.getThreshold());
        resultStr = precision(kbMap, fullReferenceMapping)    + "\t" + 
                recall(kbMap, fullReferenceMapping)        + "\t" + 
                fScore(kbMap, fullReferenceMapping)        + "\t" +
                (System.currentTimeMillis() - start)        + "\n" ;
        return resultStr;
    }

    /**
     * @param map result mapping
     * @param ref reference mapping
     * @return
     */
    protected static double recall(AMapping map, AMapping ref){
        return new Recall().calculate(map, new GoldStandard(ref));
    }

    /**
     * @param map result mapping
     * @param ref reference mapping
     * @return
     */
    protected static double fScore(AMapping map, AMapping ref){
        return new FMeasure().calculate(map, new GoldStandard(ref));
    }

    /**
     * @param map result mapping
     * @param ref reference mapping
     * @return
     */
    protected static double precision(AMapping map, AMapping ref){
        return new Precision().calculate(map, new GoldStandard(ref));
    }





    /**
     * @param args
     * @author sherif
     */
    public static void main(String[] args) {
        // evaluation parameters
        String d = "person1";
        int noisyOracleCount = 10 ;

        // get training data
        resultStr +=  d +"\nSample\tlP\tlR\tlF\tlTime\tMetricExpr\tP\tR\tF\tTime\n";
        EvaluationData data = DataSetChooser.getData(d);
        fullSourceCache = data.getSourceCache();
        fullTargetCache = data.getTargetCache();
        fullReferenceMapping = data.getReferenceMapping();


        // remove error mappings (if any)
        int refMapSize = fullReferenceMapping.size();
        fullReferenceMapping = removeLinksWithNoInstances(fullReferenceMapping);

        // training examples
        //        for(int posNegExSize = 10; posNegExSize < 100 ; posNegExSize += 10){
        int posNegExSize = 10;
        System.out.println();
        AMapping posTrainingMap = sampleReferenceMap(fullReferenceMapping, posNegExSize);
        AMapping negTrainingMap = generateNegativeExamples(posTrainingMap, posNegExSize);
        AMapping trainingMap = MappingOperations.union(posTrainingMap, negTrainingMap);

        System.out.println("trainingMap size: " + trainingMap.size());
        fillTrainingCaches(trainingMap);
        trainingMap.getReversedMap();

        // create noisy oracles with normal distribution
        
        List<NoisyOracle> noisyOracles = new ArrayList<>();
        Random pR = new Random();
        Random nR = new Random();
        double pG,nG;
        for(int i = 0 ; i < noisyOracleCount ; i++ ){
            do{
                pG = 0.75 + (pR.nextGaussian() + 1.0) / 2.0;
                nG = 0.75 + (nR.nextGaussian() + 1.0) / 2.0;
            }while(pG <0  || pG > 1 || nG <0  || nG > 1);
            noisyOracles.add(new NoisyOracle(fullReferenceMapping, pG, nG));
        }

        // initialize ligon
        Ligon ligon = new Ligon(trainingMap, sourceTrainCache, targetTrainCache, noisyOracles);
        MLResults mlResult = ligon.learn(fullSourceCache, fullTargetCache, fullReferenceMapping);
        
//        // test for the whole dataset
//        sourceTestCache = fullSourceCache;
//        targetTestCache = fullTargetCache;
//        System.out.println("-------------- Results for the whole dataset --------------\nP\tR\tF\tT" );
//        System.out.println(executeLinkSpecs(mlResult.getLinkSpecification(), sourceTestCache, targetTestCache));
        
        //        }
    }

    public static AMapping generateNegativeExamples(AMapping posExamplesMapping, int size){
        AMapping negativeExampleMapping = MappingFactory.createDefaultMapping();
        int i = 0;
        List<String> sourceUris = new ArrayList<>(posExamplesMapping.getMap().keySet());
        List<String> targetUris = new ArrayList<>();
        for(String s : posExamplesMapping.getMap().keySet()){
            targetUris.addAll(posExamplesMapping.getMap().get(s).keySet());
        }
        Random random = new Random();
        do{
            String randomSourceUri, randomTargetUri;

            do{
                randomSourceUri = sourceUris.get(random.nextInt(sourceUris.size()));
                randomTargetUri = targetUris.get(random.nextInt(targetUris.size()));
            }while(posExamplesMapping.contains(randomSourceUri, randomTargetUri));
            negativeExampleMapping.add(randomSourceUri, randomTargetUri, 0.0);
            i++;
        }while(i < size);
        return negativeExampleMapping;
    }



    public static DataSets toDataset(String d) {
        if(d.equalsIgnoreCase("DBLP-ACM")){
            return (DataSets.DBLPACM);
        }else if(d.equalsIgnoreCase("Abt-Buy")){
            return(DataSets.ABTBUY);
        }else if(d.equalsIgnoreCase("Amazon-GoogleProducts")){
            return(DataSets.AMAZONGOOGLEPRODUCTS);
        }else if(d.equalsIgnoreCase("DBLP-Scholar")){
            return(DataSets.DBLPSCHOLAR);
        }else if(d.equalsIgnoreCase("Person1")){
            return(DataSets.PERSON1);
        }else if(d.equalsIgnoreCase("Person2")){
            return(DataSets.PERSON2);
        }else if(d.equalsIgnoreCase("Restaurants")){
            return(DataSets.RESTAURANTS);
        }else if(d.equalsIgnoreCase("Restaurants_CSV")){
            return(DataSets.RESTAURANTS_CSV);
        }else if(d.equalsIgnoreCase("DBpedia-LinkedMDB")){
            return(DataSets.DBPLINKEDMDB);
        }else if(d.equalsIgnoreCase("Dailymed-DrugBank")){
            return(DataSets.DRUGS);
        }else{
            System.out.println("Experiment " + d + " Not implemented yet");
            System.exit(1);
        }
        return null;
    }

}
