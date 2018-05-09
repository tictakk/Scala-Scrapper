import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util

import scala.util.Random

object GameSimulator {

  def main(args: Array[String]): Unit ={
    val conn: Connection = DriverManager.getConnection("jdbc:mysql://"+args[0], args[1], args[2])

    //AUTOMATING GAMES FOR WEEK N
    val rs: ResultSet = conn.createStatement().executeQuery("SELECT WEEK_"+args[4]+".GAME_ID, WEEK_"+args[4]+".HOME, WEEK_"+args[4]+".AWAY FROM TEAMS LEFT JOIN WEEK_"+args[4]+" ON " +
                                                                  "(WEEK_"+args[4]+".HOME=TEAMS.TEAM_ID OR WEEK_"+args[4]+".AWAY=TEAMS.TEAM_ID) WHERE TEAMS.CONFERENCE='AAC' " +
                                                                  "AND HOME IS NOT NULL AND AWAY IS NOT NULL");

    val list = new Iterator[Game]{
      def hasNext = rs.next()
      def next() = Game(randomizeTeamData(rs.getInt(2),conn,args[4]),randomizeTeamData(rs.getInt(3),conn,args[4]))
    }.toList

    for{
      l <- list
    }yield insertMatchupData(l,conn)

    conn.close()

  }

  case class Team(name: String, id: Int, points: Int, yards: Int, home: Boolean, gameId: Int)

  case class Game(teamOne: Team, teamTwo: Team){

    def winner(): Team = {
      if (teamOne.points > teamTwo.points) teamOne else teamTwo
    }
  }

  def randomizeTeamData(id: Int, connection: Connection, week: Int): Team = {
    val st: Statement = connection.createStatement()
    val res: ResultSet = st.executeQuery("SELECT NAME FROM TEAMS WHERE TEAM_ID="+id)
    val randYards = 100 + Random.nextInt(400)
    val randScore = 7 + Random.nextInt(63)
    res.next()
    val name = res.getString(1)
    res.close()
    val rs: ResultSet = st.executeQuery("SELECT * FROM WEEK_"+week+" WHERE HOME="+id+" OR AWAY="+id)
    rs.next()
    val isHome = if(rs.getInt(2) == id)true else false
    val gameId = rs.getInt(1)
    rs.close()
    st.close()
    Team(name,id,randScore,randYards,isHome,gameId)
  }

  def insertMatchupData(game: Game, connection: Connection): Unit ={
    val st: Statement = connection.createStatement()
    st.execute("UPDATE MATCHUP SET HOME_POINTS="+game.teamOne.points+", AWAY_POINTS="+game.teamTwo.points+", HOME_OFF="+game.teamOne.yards+", AWAY_OFF="+game.teamTwo.yards+" WHERE GAME_ID="+game.teamTwo.gameId)
    st.close()
  }
}
