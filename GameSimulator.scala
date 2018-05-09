import java.sql.{Connection, DriverManager, ResultSet, Statement}
import java.util

import scala.util.Random

object GameSimulator {

  def main(args: Array[String]): Unit ={
    val conn: Connection = DriverManager.getConnection("jdbc:mysql://aa1l44kxpqxyxqa.cuapmtv2e1gk.us-east-1.rds.amazonaws.com:3306/CFB", "admin", "MooseMuffin!!22")

    //AUTOMATING GAMES FOR WEEK N
    val rs: ResultSet = conn.createStatement().executeQuery("SELECT WEEK_"+3+".GAME_ID, WEEK_"+3+".HOME, WEEK_"+3+".AWAY FROM TEAMS LEFT JOIN WEEK_"+3+" ON " +
                                                                  "(WEEK_"+3+".HOME=TEAMS.TEAM_ID OR WEEK_"+3+".AWAY=TEAMS.TEAM_ID) WHERE TEAMS.CONFERENCE='AAC' " +
                                                                  "AND HOME IS NOT NULL AND AWAY IS NOT NULL");

    val list = new Iterator[Game]{
      def hasNext = rs.next()
      def next() = Game(randomizeTeamData(rs.getInt(2),conn,3),randomizeTeamData(rs.getInt(3),conn,3))
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
//    println("UPDATE MATCHUP SET HOME_POINTS="+game.teamOne.points+", AWAY_POINTS="+game.teamTwo.points+", HOME_OFF="+game.teamOne.yards+", AWAY_0FF="+game.teamTwo.yards+" WHERE GAME_ID="+game.teamTwo.gameId)
    st.execute("UPDATE MATCHUP SET HOME_POINTS="+game.teamOne.points+", AWAY_POINTS="+game.teamTwo.points+", HOME_OFF="+game.teamOne.yards+", AWAY_OFF="+game.teamTwo.yards+" WHERE GAME_ID="+game.teamTwo.gameId)
    st.close()
  }
}
