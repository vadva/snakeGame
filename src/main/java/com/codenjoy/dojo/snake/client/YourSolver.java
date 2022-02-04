package com.codenjoy.dojo.snake.client;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2018 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.snake.model.Elements;

import java.util.*;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {
    private Dice dice;
    private Board board;
    private Node graph[][];
    private boolean isShaped = false;
    public int countVisitsSnakeElements = 0;

    Node goodApple;
    Node headTail;
    Node destination;

    public Node getHeadTail() {
        return headTail;
    }

    public int getCountVisitsSnakeElements() {
        return countVisitsSnakeElements;
    }

    public void setCountVisitsSnakeElements(int countVisitsSnakeElements) {
        this.countVisitsSnakeElements = countVisitsSnakeElements;
    }

    public boolean isShaped() {
        return isShaped;
    }

    public void setShaped(boolean shaped) {
        isShaped = shaped;
    }

    private class Node {

        private boolean visited;
        private int countVisits = 0;


        private ArrayList<Integer> countListVisits = new ArrayList<>(Arrays.asList(0, 0, 0, 0));

        private HashMap<Direction, Node> neighbors;
        private Point point;
        private int peekValue = 1000;

        public Node(Point point) {
            this.point = point;
            this.neighbors = new HashMap<>();
            this.visited = false;
        }

        public ArrayList<Integer> getCountListVisits() {
            return countListVisits;
        }

        public void setCountListVisits(ArrayList<Integer> countListVisits) {
            this.countListVisits = countListVisits;
        }

        public int getPeekValue() {
            return peekValue;
        }

        public void setPeekValue(int peekValue) {
            this.peekValue = peekValue;
        }

        public void setVisited(boolean visited) {
            this.visited = visited;
        }

        public boolean isVisited() {
            return visited;
        }

        public HashMap<Direction, Node> getNeighbors() {
            return neighbors;
        }

        public Point getPoint() {
            return point;
        }

        public int getCountVisits() {
            return countVisits;
        }

        public void setCountVisits(int countVisits) {
            this.countVisits = countVisits;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return visited == node.visited && Objects.equals(neighbors, node.neighbors) && Objects.equals(point, node.point);
        }

        @Override
        public int hashCode() {
            return Objects.hash(visited, neighbors, point);
        }

        @Override
        public String toString() {
            return "Node{" +
                    "visited=" + visited +
                    ", neighbors=" + neighbors +
                    ", point=" + point +
                    '}';
        }
    }

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    private String printGraph() {
        for (int y = board.size() - 2; y > 0; y--) {
            System.out.println();
            for (int x = 1; x < board.size() - 1; x++) {
                if (graph[x][y] != null) {
                    System.out.print("[" + graph[x][y].point.getX() + ",");
                    System.out.print(graph[x][y].point.getY() + "]");
                    System.out.printf("%7d\t", graph[x][y].getPeekValue());
                } else {
                    System.out.print("[null]          ");
                }
            }
        }
        return "null";
    }

    private Node[][] createGraph() {
        for (int x = 1; x < board.size(); x++) {
            for (int y = 1; y < board.size(); y++) {
                if (board.isAt(x, y, Elements.NONE, Elements.GOOD_APPLE, Elements.HEAD_RIGHT, Elements.HEAD_UP, Elements.HEAD_LEFT, Elements.HEAD_DOWN)) {
                    graph[x][y] = new Node(new PointImpl(x, y));
                    if (graph[x - 1][y] != null) {
                        graph[x - 1][y].neighbors.put(Direction.RIGHT, graph[x][y]);
                        graph[x][y].neighbors.put(Direction.LEFT, graph[x - 1][y]);
                    }
                    if (graph[x + 1][y] != null) {
                        graph[x + 1][y].neighbors.put(Direction.LEFT, graph[x][y]);
                        graph[x][y].neighbors.put(Direction.RIGHT, graph[x + 1][y]);
                    }
                    if (graph[x][y - 1] != null) {
                        graph[x][y - 1].neighbors.put(Direction.UP, graph[x][y]);
                        graph[x][y].neighbors.put(Direction.DOWN, graph[x][y - 1]);
                    }
                    if (graph[x][y + 1] != null) {
                        graph[x][y + 1].neighbors.put(Direction.DOWN, graph[x][y]);
                        graph[x][y].neighbors.put(Direction.UP, graph[x][y + 1]);
                    }
                }
            }
        }
        return graph;
    }

    private void setAllPeekValue(Node dest, int count) {
        count++;

        Set<Map.Entry<Direction, Node>> set = dest.neighbors.entrySet();

        if (dest != null && !dest.isVisited() && dest.getPeekValue() > count) {
            dest.setPeekValue(count);
            dest.setVisited(true);
            for (Map.Entry<Direction, Node> me : set) {
                setAllPeekValue(dest.neighbors.get(me.getKey()), count);
                dest.setVisited(false);
            }
        }
    }

    private String findWay() {
        Direction priorityWay = Direction.RIGHT;

        int currentDistance = 0;
        int shortDistance = 1000;

        Set<Map.Entry<Direction, Node>> set = headTail.neighbors.entrySet();

        for (Map.Entry<Direction, Node> me : set) {
            currentDistance = me.getValue().getPeekValue();
            if (currentDistance < shortDistance) {
                priorityWay = me.getKey();
                shortDistance = currentDistance;
            }
        }

        if (currentDistance == 1000) {

             graph[board.getStones().get(0).getX()][board.getStones().get(0).getY()] = new Node(new PointImpl(board.getStones().get(0).getX(), board.getStones().get(0).getY()));

            Set<Map.Entry<Direction, Node>> pull = this.getHeadTail().neighbors.entrySet();

            for (Map.Entry<Direction, Node> me : pull) {
                priorityWay = me.getKey();
            }
        }
        return priorityWay.toString();
    }

    @Override
    public String get(Board board) {
        this.board = board;
        this.graph = new Node[board.size()][board.size()];
        createGraph();

        this.destination = graph[board.getApples().get(0).getX()][board.getApples().get(0).getY()];
        this.goodApple = graph[board.getApples().get(0).getX()][board.getApples().get(0).getY()];
        this.headTail = graph[board.getHead().getX()][board.getHead().getY()];

        setAllPeekValue(this.destination, 0);

        printGraph();

        return findWay();

    }

    public static void main(String[] args) {

        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://164.90.213.43/codenjoy-contest/board/player/s5368ao1u4vwlgxy7e63?code=2329602366841854203",
                new YourSolver(new RandomDice()),
                new Board());
    }

    ;
}
