package com.uoc.tennis.AI;

import android.content.Context;
import android.content.res.AssetManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.BinomialModelPrediction;

public class Model {

    public static StrokeTypes getPrediction(HashMap<String, Object> dataToPredict, Context context) throws Exception {

        AssetManager mgr = context.getApplicationContext().getResources().getAssets();
        EasyPredictModelWrapper model = new EasyPredictModelWrapper(MojoModel.load(mgr.list("model")[0]));

        RowData row = (RowData) dataToPredict;

        BinomialModelPrediction p = model.predictBinomial(row);
        List probabilities = Collections.singletonList(p.classProbabilities);
        Double max = probabilities
                .stream()
                .mapToDouble(v -> (double) v)
                .max()
                .orElseThrow(Exception::new);

        return StrokeTypes.values()[probabilities.indexOf(max)];
    }

}


