import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.Factory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ShipRouteOptimization {

    // Parametry planszy i liczba przeszkod
    private static final int GRID_SIZE = 20; // Rozmiar planszy 20x20
    private static final int OBSTACLE_COUNT = 30; // Liczba losowych przeszkod
    private static final Point START = new Point(0, 0);
    private static final Point END = new Point(GRID_SIZE - 1, GRID_SIZE - 1);

    // Zbior losowych przeszkod
    private static Set<Point> obstacles = new HashSet<>();

    public static void main(String[] args) {
        // Generowanie losowych przeszkod
        generateRandomObstacles();

        // Konfiguracja genotypu - sekwencja ruchow (0-3 reprezentuja rozne kierunki)
        Factory<Genotype<IntegerGene>> genotypeFactory = Genotype.of(
                IntegerChromosome.of(0, 3, 100) // Ustawiamy maksymalnie 100 krokow
        );

        // Konfiguracja silnika ewolucyjnego
        Engine<IntegerGene, Double> engine = Engine.builder(ShipRouteOptimization::fitness, genotypeFactory)
                .populationSize(200)
                .optimize(Optimize.MAXIMUM)
                .alterers(
                        new Mutator<>(0.2), // Mutacja
                        new SinglePointCrossover<>(0.3) // Krzyzowanie
                )
                .build();

        // Uruchomienie ewolucji
        Phenotype<IntegerGene, Double> result = engine.stream()
                .limit(100)
                .collect(EvolutionResult.toBestPhenotype());

        // Wypisanie wyniku
        System.out.println("Wartosc fitness: " + result.fitness());
    }

    // Generowanie losowych przeszkod na planszy
    private static void generateRandomObstacles() {
        Random random = new Random(); // Domyślny generator oparty na czasie
        obstacles.clear();

        while (obstacles.size() < OBSTACLE_COUNT) {
            int x = random.nextInt(GRID_SIZE);
            int y = random.nextInt(GRID_SIZE);
            Point obstacle = new Point(x, y);

            // Unikamy kolizji z punktami startowym i koncowym
            if (!obstacle.equals(START) && !obstacle.equals(END)) {
                obstacles.add(obstacle);
            }
        }
    }

    // Funkcja fitness ocenia trasę
    private static double fitness(Genotype<IntegerGene> genotype) {
        List<Point> route = decode(genotype);
        double distance = calculateDistance(route);
        return 1 / (distance + calculateBacktrackingPenalty(route));
    }

    // Dekodowanie genotypu do listy punktow trasy
    private static List<Point> decode(Genotype<IntegerGene> genotype) {
        List<Point> route = new ArrayList<>();
        route.add(START);
        Point current = START;

        for (IntegerGene gene : genotype.chromosome()) {
            // Kazdy gen reprezentuje ruch w jednym z 4 kierunkow: prawo, lewo, gora, dol
            int direction = gene.intValue();
            Point next = switch (direction) {
                case 0 -> new Point(current.x + 1, current.y); // Prawo
                case 1 -> new Point(current.x - 1, current.y); // Lewo
                case 2 -> new Point(current.x, current.y + 1); // Dol
                case 3 -> new Point(current.x, current.y - 1); // Gora
                default -> current;
            };

            // Sprawdza czy nowy punkt jest w zakresie, unikalny, nie jest przeszkoda i dodaj do trasy
            if (isValidPoint(next)) {
                route.add(next);
                current = next;
            } else {
                // Jeśli ruch prowadzi poza plansze lub do przeszkody nie wykonujemy go
                // Mozemy po prostu zatrzymać sie na tym samym punkcie
                route.add(current);
            }
        }

        // Na koncu dodaj punkt koncowy, aby upewnić się, ze trasa dochodzi do konca
        route.add(END);
        return route;
    }

    // Sprawdzenie, czy punkt jest w zakresie i nie jest przeszkoda
    private static boolean isValidPoint(Point p) {
        return p.x >= 0 && p.y >= 0 && p.x < GRID_SIZE && p.y < GRID_SIZE && !obstacles.contains(p);
    }

    // Obliczanie calkowitego dystansu trasy
    private static double calculateDistance(List<Point> route) {
        double distance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            distance += route.get(i).distance(route.get(i + 1));
        }
        return distance;
    }

    // Kara za cofanie się lub powtarzanie punktow
    private static double calculateBacktrackingPenalty(List<Point> route) {
        double penalty = 0;
        for (int i = 1; i < route.size() - 1; i++) {
            Point prev = route.get(i - 1);
            Point curr = route.get(i);
            Point next = route.get(i + 1);
            if (curr.equals(prev) || curr.equals(next)) {
                penalty += 500; // Kara za cofanie lub stanie w miejscu
            }
        }
        return penalty;
    }
}