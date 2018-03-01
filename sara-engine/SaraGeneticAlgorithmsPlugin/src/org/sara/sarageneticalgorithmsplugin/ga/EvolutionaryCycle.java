package org.sara.sarageneticalgorithmsplugin.ga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.sara.sarageneticalgorithmsplugin.ga.factory.RandomPopulationFactory;
import org.sara.interfaces.ICore;
import org.sara.interfaces.ga.factory.IPopulationFactory;
import org.sara.interfaces.algorithms.ga.IGAEngine;
import org.sara.interfaces.algorithms.ga.model.ISpecimen;
import org.sara.interfaces.algorithms.ga.operator.ICrossover;
import org.sara.interfaces.algorithms.ga.operator.IFitness;
import org.sara.interfaces.algorithms.ga.operator.IGALightSwitch;
import org.sara.interfaces.algorithms.ga.operator.IMutation;
import org.sara.interfaces.algorithms.ga.model.IPopulation;
import org.sara.interfaces.algorithms.ga.operator.ISelection;
import org.sara.interfaces.model.GAConfiguration;
import org.sara.interfaces.model.InfoSolution;
import org.sara.interfaces.model.Slot;
import org.sara.sarageneticalgorithmsplugin.ga.factory.ExistingSolutionPopulationFactory;
import org.sara.sarageneticalgorithmsplugin.operator.crossover.RandomCrossover;
import org.sara.sarageneticalgorithmsplugin.operator.crossover.TwoPointCrossover;
import org.sara.sarageneticalgorithmsplugin.operator.crossover.UniformCrossover;
import org.sara.sarageneticalgorithmsplugin.operator.fitness.IFBACAFitness;
import org.sara.sarageneticalgorithmsplugin.ga.model.Generation;
import org.sara.sarageneticalgorithmsplugin.operator.crossover.GreatestCrossover;
import org.sara.sarageneticalgorithmsplugin.operator.mutation.SwapMutation;
import org.sara.sarageneticalgorithmsplugin.operators.selection.RankingSelection;
import org.sara.sarageneticalgorithmsplugin.operator.galightswitch.GALightSwitch;
import org.sara.sarageneticalgorithmsplugin.operator.mutation.RandomMutation;
import org.sara.sarageneticalgorithmsplugin.operators.selection.RandomSelection;
import org.sara.sarageneticalgorithmsplugin.operators.selection.TournamentSelection;

public class EvolutionaryCycle implements IGAEngine {

    public EvolutionaryCycle() {
        System.out.println("Engine EvolutionaryCycle was created.");

        ISelection[] selections = {new RankingSelection(), new TournamentSelection()};
        ICrossover[] crossovers = {new TwoPointCrossover(), new UniformCrossover(), new GreatestCrossover()};
        IMutation[] mutations = {new SwapMutation()};

        this.gaConfiguration = ICore.getInstance().getModelController().getGaConfiguration();
        this.gaConfiguration.setGaLightSwitch(new GALightSwitch(gaConfiguration.getMaxGeneration()));

        this.gaConfiguration.setSelection(new RandomSelection(selections));
        this.gaConfiguration.setCrossover(new RandomCrossover(crossovers));
        this.gaConfiguration.setMutation(new RandomMutation(mutations));
        this.gaConfiguration.setFitness(new IFBACAFitness());
    }

    @Override
    public InfoSolution startCycle() {
        IPopulation population;

        //Get the current operators
        IFitness fitness = this.gaConfiguration.getFitness();
        ISelection selection = this.gaConfiguration.getSelection();
        ICrossover crossover = this.gaConfiguration.getCrossover();
        IMutation mutation = this.gaConfiguration.getMutation();
        IGALightSwitch terminate = this.gaConfiguration.getGaLightSwitch();
        IPopulationFactory populationFactory = RandomPopulationFactory.getInstance();

        int genNumber = 1;
        long timeOfGenerateInitialPopulation;
        long averageTimeOfFitness;
        long averageTimeOfSelection;
        long averageTimeOfCrossover;
        long averageTimeOfMutation;
        long averageTimeOfRefreshPopulation;
        Date startDate, endDate;
        
        startDate = new Date();
        //Gera a população inicial
            population = populationFactory.makePopulation();
        //End
        endDate = new Date();
        timeOfGenerateInitialPopulation = endDate.getTime() - startDate.getTime();
        
        do {
            startDate = new Date();
            //Calcula o fitness de cada indivíduo
                fitness.evaluate(population);
            //End
            endDate = new Date();
            averageTimeOfFitness = endDate.getTime() - startDate.getTime();
            if(genNumber > 1)
                averageTimeOfFitness /= 2;
            
            startDate = new Date();
            //Garante o Elitismo (Uma parte dos melhores indivíduos dos genitores
                List<ISpecimen> elite = new ArrayList<>();
                elite.addAll(population.getBetterSpecimens((int) (population.size() * this.gaConfiguration.getElitismProbability()), true));
            //end
            
            //Seleciona os genitores (parents)
                selection.select(population, this.gaConfiguration.getSelectProbability());
            //End
            endDate = new Date();
            averageTimeOfSelection = endDate.getTime() - startDate.getTime();
            if(genNumber > 1)
                averageTimeOfSelection /= 2;
            
            startDate = new Date();
            //Cruza os genitores e gera os descendentes (offSpring)
                ((RandomCrossover) crossover).changeMode();
                crossover.makeOffspring(population);
            //End 
            endDate = new Date();
            averageTimeOfCrossover = endDate.getTime() - startDate.getTime();
            if(genNumber > 1)
                averageTimeOfCrossover /= 2;
            
            startDate = new Date();
            //Gera a mutação em cima dos genitores
                mutation.mutate(population, this.gaConfiguration.getMutationProbability());
            //End
            endDate = new Date();
            averageTimeOfMutation = endDate.getTime() - startDate.getTime();
            if(genNumber > 1)
                averageTimeOfMutation /= 2;
            
            startDate = new Date();
            //Atualiza População
                population.sortByFitness();
                population.removeLastSpecimen(elite.size());
                population.addSpecimens(elite, true);
                elite.clear();
            //End
            endDate = new Date();
            averageTimeOfRefreshPopulation = endDate.getTime() - startDate.getTime();
            if(genNumber > 1)
                averageTimeOfRefreshPopulation /= 2;
        } while (!terminate.stop(new Generation(genNumber++, population)));
        
        
        //gravando os meta dados da execução
        InfoSolution info = new InfoSolution();
        info.setBestSolution(terminate.getBestSolution());
        info.setFitnessTimeLine(terminate.getFitnessTimeLine());
        info.setAverageTimeOfFitness(averageTimeOfFitness);
        info.setTimeOfGenerateInitialPopulation(timeOfGenerateInitialPopulation);
        info.setAverageTimeOfCrossover(averageTimeOfCrossover);
        info.setAverageTimeOfSelection(averageTimeOfSelection);
        info.setAverageTimeOfMutation(averageTimeOfMutation);
        info.setAverageTimeOfRefreshPopulation(averageTimeOfRefreshPopulation);
                
        return info;
    }

    @Override
    public InfoSolution evalSolution(Collection chromossomes) {
        
        if (!(chromossomes instanceof Collection)) {
            System.err.printf("Genetic Load is invalid!");
            System.exit(1);
        }
        
        List specimen = new ArrayList<>((Collection) chromossomes);
        IFitness fitness = this.gaConfiguration.getFitness();
        long timeOfGenerateInitialPopulation;
        long averageTimeOfFitness;
        Date startDate, endDate;
        InfoSolution info;
        
        startDate = new Date();
        //Gera a população inicial
            ExistingSolutionPopulationFactory.getInstance().setSpecimenData(specimen);
            IPopulation population = ExistingSolutionPopulationFactory.getInstance().makePopulation();
        //End
        endDate = new Date();
        timeOfGenerateInitialPopulation = endDate.getTime() - startDate.getTime(); 
        
        
        //Calcula o fitness de cada indivíduo
            ICore.getInstance().getModelController().getGaConfiguration().setPopulationNumber(1);
            fitness.evaluate(population);
        //End
        endDate = new Date();
        averageTimeOfFitness = endDate.getTime() - startDate.getTime();
        
        List<Object> slots = new ArrayList<>();
        population.getBestSpecimen(false).getAllGenes(false).forEach((gene) -> slots.add((Slot) gene.getAllele(false)));
        
        info = new InfoSolution();
        info.setAverageTimeOfFitness(averageTimeOfFitness);
        info.setTimeOfGenerateInitialPopulation(timeOfGenerateInitialPopulation);
        info.setBestSolution(slots);
        info.setFitnessOfTheBestSolution(population.getBestSpecimen(false).getFitness());

        return info;
    }
    
    private final GAConfiguration gaConfiguration;
}
