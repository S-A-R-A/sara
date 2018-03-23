package org.sara.interfaces.algorithms.ga.operator;

import java.util.List;
import org.sara.interfaces.algorithms.ga.model.IChromosome;
import org.sara.interfaces.algorithms.ga.model.IGeneration;

public interface IGALightSwitch {

    public boolean stop(IGeneration generation);
    public List<IChromosome> getBestSolution();
    public List<Float> getFitnessTimeLine();
    public Float getBestFitness();
}
