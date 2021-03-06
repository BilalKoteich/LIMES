package org.aksw.limes.core.evaluation;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.aksw.limes.core.datastrutures.EvaluationRun;
import org.aksw.limes.core.datastrutures.TaskAlgorithm;
import org.aksw.limes.core.datastrutures.TaskData;
import org.aksw.limes.core.evaluation.evaluator.Evaluator;
import org.aksw.limes.core.evaluation.evaluator.EvaluatorType;
import org.aksw.limes.core.ml.algorithm.SupervisedMLAlgorithm;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvaluatorsTest {
    static Logger logger = LoggerFactory.getLogger(EvaluatorsTest.class);


    final private String[] datasetsList = {"RESTAURANTS"/*,"PERSON1",  "PERSON2", "PERSON1_CSV", "PERSON2_CSV", "OAEI2014BOOKS"*/};
    final private String[] algorithmsListData = {"UNSUPERVISED:WOMBATSIMPLE","SUPERVISED_BATCH:WOMBATSIMPLE","SUPERVISED_ACTIVE:WOMBATSIMPLE","UNSUPERVISED:WOMBATCOMPLETE","SUPERVISED_BATCH:WOMBATCOMPLETE"};

    private static final int folds=10;
    private static final boolean crossValidate=false; 

    @Test
    public void test() {
        /*        if(crossValidate)
            testCrossValidate();
        else*/
        //        testEvaluator();
        testCrossValidate(); //TODO: FIX
    }
    public void testEvaluator() {
        try {

            DatasetsInitTest ds = new DatasetsInitTest();
            EvaluatorsInitTest ev = new EvaluatorsInitTest();
            AlgorithmsInitTest al = new AlgorithmsInitTest();
            Evaluator evaluator = new Evaluator();

            Set<TaskData> tasks = ds.initializeDataSets(datasetsList);
            Set<EvaluatorType> evaluators = ev.initializeEvaluators();
            List<TaskAlgorithm> algorithms = al.initializeMLAlgorithms(algorithmsListData,datasetsList.length);

            List<EvaluationRun> results = evaluator.evaluate(algorithms, tasks, evaluators, null);
            for (EvaluationRun er : results) {
                er.display();
            }
            for (EvaluationRun er : results) {
                System.out.println(er);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertTrue(false);
        }
        assertTrue(true);
    }


    public void testCrossValidate() {


        DatasetsInitTest ds = new DatasetsInitTest();
        EvaluatorsInitTest ev = new EvaluatorsInitTest();
        AlgorithmsInitTest al = new AlgorithmsInitTest();
        Evaluator evaluator = new Evaluator();

        Set<TaskData> tasks = ds.initializeDataSets(datasetsList);
        Set<EvaluatorType> evaluators = ev.initializeEvaluators();
        List<TaskAlgorithm> algorithms = al.initializeMLAlgorithms(algorithmsListData,datasetsList.length);
        List<EvaluationRun> results =null;
        for (TaskAlgorithm tAlgorithm : algorithms) {
            System.out.println("testing "+tAlgorithm.getMlAlgorithm().toString());
            if(tAlgorithm.getMlAlgorithm() instanceof SupervisedMLAlgorithm)
                results = evaluator.crossValidate(tAlgorithm.getMlAlgorithm(), null, tasks,folds, evaluators, null);
            System.out.println(results);
        }
        assertTrue(true);
    }


}
